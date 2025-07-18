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

package android.tools.flicker.config.splitscreen

import android.tools.flicker.config.AssertionTemplates
import android.tools.flicker.config.FlickerConfigEntry
import android.tools.flicker.config.ScenarioId
import android.tools.flicker.config.TransitionFilters
import android.tools.flicker.extractors.TaggedCujTransitionMatcher
import android.tools.flicker.extractors.TaggedScenarioExtractorBuilder
import android.tools.traces.events.CujType

val SplitScreenResize =
    FlickerConfigEntry(
        enabled = true,
        scenarioId = ScenarioId("SPLIT_SCREEN_RESIZE"),
        assertions = AssertionTemplates.RESIZE_SPLITSCREEN_ASSERTIONS,
        extractor =
            TaggedScenarioExtractorBuilder()
                .setTargetTag(CujType.CUJ_SPLIT_SCREEN_RESIZE)
                .setTransitionMatcher(
                    TaggedCujTransitionMatcher(
                        TransitionFilters.RESIZE_SPLIT_SCREEN_FILTER,
                        // No match will be found when resizing all the way to dismissing
                        associatedTransitionRequired = false,
                    )
                )
                // If we don't find a matching transition, we probably dismissed splitscreen so
                // don't consider as a splitscreen resize scenario.
                .setIgnoreIfNoMatchingTransition(true)
                .build(),
    )
