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

package android.tools.flicker.assertors.assertions

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.helpers.WindowUtils

class AppWindowCoversRightHalfScreenAtEnd(
    private val component: ComponentTemplate,
    private val coverageDifferenceThresholdRatio: Double? = null,
) : AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        flicker.assertWmEnd {
            if (coverageDifferenceThresholdRatio == null) {
                // Build expected bounds of half the display
                val expectedBounds =
                    WindowUtils.getInsetDisplayBounds(scenarioInstance.startRotation).apply {
                        left = centerX()
                    }
                visibleRegion(component.get(scenarioInstance)).coversExactly(expectedBounds)
            } else {
                // Build expected bounds of half the display (minus given threshold)
                val expectedBounds =
                    WindowUtils.getInsetDisplayBounds(scenarioInstance.startRotation).apply {
                        left = (centerX() * (1 + coverageDifferenceThresholdRatio)).toInt()
                    }
                visibleRegion(component.get(scenarioInstance)).coversAtLeast(expectedBounds)
            }
        }
    }
}
