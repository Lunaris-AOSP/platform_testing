/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.tools.flicker.assertors.assertions

import android.graphics.Region
import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.flicker.config.splitscreen.Components.SPLIT_SCREEN_DIVIDER
import android.tools.flicker.subject.layers.LayerTraceEntrySubject
import android.tools.traces.wm.WindowManagerTrace

class SplitAppLayerBoundsSnapToDivider(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        val wmTrace = scenarioInstance.reader.readWmTrace() ?: return

        val matcher = component.get(scenarioInstance)
        flicker.assertLayers {
            invoke("splitAppLayerBoundsSnapToDivider") {
                it.visibleRegion(matcher)
                    .coversAtMost(it.calculateExpectedDisplaySize(scenarioInstance, wmTrace))
            }
        }
    }

    companion object {
        private fun LayerTraceEntrySubject.calculateExpectedDisplaySize(
            scenarioInstance: ScenarioInstance,
            wmTrace: WindowManagerTrace,
        ): Region {
            // TODO: Replace with always on tracing available data
            val landscapePosLeft = !wmTrace.isTablet
            val portraitPosTop = true // TODO: Figure out how to know if we are top or bottom app

            val splitScreenDivider = SPLIT_SCREEN_DIVIDER.get(scenarioInstance)

            val displaySize =
                entry.displays
                    .first()
                    .size // TODO: Better way of getting correct display instead of just first
            val dividerRegion =
                layer(splitScreenDivider)?.visibleRegion?.region
                    ?: error("Missing splitscreen divider")

            require(!dividerRegion.isEmpty) { "Splitscreen divider region should not be empty" }

            return if (displaySize.width > displaySize.height) {
                if (landscapePosLeft) {
                    Region(
                        0,
                        0,
                        (dividerRegion.bounds.left + dividerRegion.bounds.right) / 2,
                        displaySize.height,
                    )
                } else {
                    Region(
                        (dividerRegion.bounds.left + dividerRegion.bounds.right) / 2,
                        0,
                        displaySize.width,
                        displaySize.height,
                    )
                }
            } else {
                if (portraitPosTop) {
                    Region(
                        0,
                        0,
                        displaySize.width,
                        (dividerRegion.bounds.top + dividerRegion.bounds.bottom) / 2,
                    )
                } else {
                    Region(
                        0,
                        (dividerRegion.bounds.top + dividerRegion.bounds.bottom) / 2,
                        displaySize.width,
                        displaySize.height,
                    )
                }
            }
        }
    }
}
