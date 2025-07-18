/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tools.flicker.extractors

import android.tools.flicker.extractors.TransitionTransforms.inCujRangeFilter
import android.tools.flicker.extractors.TransitionTransforms.mergeTrampolineTransitions
import android.tools.flicker.extractors.TransitionTransforms.noOpTransitionsTransform
import android.tools.flicker.extractors.TransitionTransforms.permissionDialogFilter
import android.tools.io.Reader
import android.tools.traces.events.Cuj
import android.tools.traces.wm.Transition
import android.tools.traces.wm.TransitionType

typealias TransitionsTransform =
    (transitions: List<Transition>, cujEntry: Cuj, reader: Reader) -> List<Transition>

class TaggedCujTransitionMatcher(
    private val mainTransform: TransitionsTransform = noOpTransitionsTransform,
    private val finalTransform: TransitionsTransform = noOpTransitionsTransform,
    private val associatedTransitionRequired: Boolean = true,
) : TransitionMatcher {
    override fun getMatches(reader: Reader, cujEntry: Cuj): Collection<Transition> {
        val transformsToNames: Map<TransitionsTransform, String> =
            mapOf(
                mainTransform to "Main Transform",
                inCujRangeFilter to "In CUJ Range Filter",
                permissionDialogFilter to "Permission Dialog Filter",
                mergeTrampolineTransitions to "Merge Trampoline Transitions",
                finalTransform to "Final Transform",
            )
        val transforms = transformsToNames.keys

        val transitionsTrace = reader.readTransitionsTrace() ?: error("Missing transitions trace")

        val completeTransitions = transitionsTrace.entries.filter { !it.isIncomplete }

        val formattedTransitions = { transitions: Collection<Transition> ->
            "[\n" +
                "${
                    transitions.joinToString(",\n") {
                            Transition.Formatter(reader.readLayersTrace(),
                                    reader.readWmTrace(),).format(it)
                        }.prependIndent()
                    }\n" +
                "]"
        }

        require(!associatedTransitionRequired || completeTransitions.isNotEmpty()) {
            "No successfully finished transitions in: " +
                formattedTransitions(transitionsTrace.entries)
        }

        var appliedTransformsCount = 0
        val matchedTransitions =
            transforms.fold(completeTransitions) { transitions, transform ->
                val remainingTransitions =
                    try {
                        transform(transitions, cujEntry, reader)
                    } catch (e: Exception) {
                        throw RuntimeException(
                            "Failed to apply ${transformsToNames[transform]} on " +
                                "the following transitions (CUJ=${cujEntry.cuj.name}" +
                                "[${cujEntry.startTimestamp},${cujEntry.endTimestamp}]):\n " +
                                formattedTransitions(transitions),
                            e,
                        )
                    }

                appliedTransformsCount++

                require(!associatedTransitionRequired || remainingTransitions.isNotEmpty()) {
                    "Required an associated transition for ${cujEntry.cuj.name}" +
                        "(${cujEntry.startTimestamp},${cujEntry.endTimestamp}) " +
                        "but no transition left after applying ${transformsToNames[transform]} " +
                        "($appliedTransformsCount/${transforms.size} filters) " +
                        "from the following transitions: " +
                        formattedTransitions(transitionsTrace.entries)
                }

                remainingTransitions
            }

        require(!associatedTransitionRequired || matchedTransitions.size == 1) {
            "Got too many associated transitions for CUJ $cujEntry expected only 1, but got: [\n" +
                "${matchedTransitions.joinToString(",\n")}\n]"
        }

        return matchedTransitions
    }
}

object TransitionTransforms {
    val inCujRangeFilter: TransitionsTransform = { transitions, cujEntry, reader ->
        transitions.filter { transition ->
            val transitionCreatedWithinCujTags =
                cujEntry.startTimestamp <= transition.createTime &&
                    transition.createTime <= cujEntry.endTimestamp

            val transitionSentWithinCujTags =
                cujEntry.startTimestamp <= transition.sendTime &&
                    transition.sendTime <= cujEntry.endTimestamp

            val transactionsTrace =
                reader.readTransactionsTrace() ?: error("Missing transactions trace")
            val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
            val finishTransaction = transition.getFinishTransaction(transactionsTrace)
            val transitionEndTimestamp =
                if (finishTransaction != null) {
                    layersTrace.getEntryForTransaction(finishTransaction).timestamp
                } else {
                    transition.finishTime
                }
            val cujStartsDuringTransition =
                transition.sendTime <= cujEntry.startTimestamp &&
                    cujEntry.startTimestamp <= transitionEndTimestamp
            return@filter transitionCreatedWithinCujTags ||
                transitionSentWithinCujTags ||
                cujStartsDuringTransition
        }
    }

    val permissionDialogFilter: TransitionsTransform = { transitions, _, reader ->
        transitions.filter { !isPermissionDialogOpenTransition(it, reader) }
    }

    val mergeTrampolineTransitions: TransitionsTransform = { transitions, _, reader ->
        require(transitions.size <= 2) {
            "Got to merging trampoline transitions with more than 2 transitions left :: " +
                transitions.joinToString {
                    Transition.Formatter(reader.readLayersTrace(), reader.readWmTrace()).format(it)
                }
        }
        if (
            transitions.size == 2 &&
                isTrampolinedOpenTransition(
                    transitions.first(),
                    transitions.drop(1).first(),
                    reader,
                )
        ) {
            // Remove the trampoline transition
            listOf(transitions.first())
        } else {
            transitions
        }
    }

    val noOpTransitionsTransform: TransitionsTransform = { transitions, _, _ -> transitions }

    private fun isPermissionDialogOpenTransition(transition: Transition, reader: Reader): Boolean {
        if (transition.changes.size != 1) {
            return false
        }

        val change = transition.changes.first()
        if (transition.type != TransitionType.OPEN || change.transitMode != TransitionType.OPEN) {
            return false
        }

        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val layers = layersTrace.entries.flatMap { it.flattenedLayers }.distinctBy { it.id }

        val candidateLayer =
            layers.firstOrNull { it.id == change.layerId }
                ?: error("Open layer from $transition not found in layers trace")
        return candidateLayer.name.contains("permissioncontroller")
    }

    private fun isTrampolinedOpenTransition(
        firstTransition: Transition,
        secondTransition: Transition,
        reader: Reader,
    ): Boolean {
        val candidateTaskLayers =
            firstTransition.changes
                .filter {
                    it.transitMode == TransitionType.OPEN ||
                        it.transitMode == TransitionType.TO_FRONT
                }
                .map { it.layerId }
        if (candidateTaskLayers.isEmpty()) {
            return false
        }

        require(candidateTaskLayers.size == 1) {
            "Unhandled case (more than 1 task candidate) in isTrampolinedOpenTransition()"
        }

        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val layers = layersTrace.entries.flatMap { it.flattenedLayers }.distinctBy { it.id }

        val candidateTaskLayerId = candidateTaskLayers.first()
        val candidateTaskLayer = layers.first { it.id == candidateTaskLayerId }
        if (!candidateTaskLayer.name.contains("Task")) {
            return false
        }

        val candidateTrampolinedActivities =
            secondTransition.changes
                .filter { it.transitMode == TransitionType.CLOSE }
                .map { it.layerId }
        val candidateTargetActivities =
            secondTransition.changes
                .filter {
                    it.transitMode == TransitionType.OPEN ||
                        it.transitMode == TransitionType.TO_FRONT
                }
                .map { it.layerId }
        if (candidateTrampolinedActivities.isEmpty() || candidateTargetActivities.isEmpty()) {
            return false
        }

        require(candidateTargetActivities.size == 1) {
            "Unhandled case (more than 1 trampolined candidate) in " +
                "isTrampolinedOpenTransition()"
        }
        require(candidateTargetActivities.size == 1) {
            "Unhandled case (more than 1 target candidate) in isTrampolinedOpenTransition()"
        }

        val candidateTrampolinedActivityId = candidateTargetActivities.first()
        val candidateTrampolinedActivity = layers.first { it.id == candidateTrampolinedActivityId }
        if (candidateTrampolinedActivity.parent?.id != candidateTaskLayerId) {
            return false
        }

        val candidateTargetActivityId = candidateTargetActivities.first()
        val candidateTargetActivity = layers.first { it.id == candidateTargetActivityId }
        if (candidateTargetActivity.parent?.id != candidateTaskLayerId) {
            return false
        }

        return true
    }
}
