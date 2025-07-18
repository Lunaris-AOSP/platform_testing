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

/**
 * Checks that [component] window is pinned and visible at the start and then becomes unpinned and
 * invisible at the same moment, and remains unpinned and invisible until the end of the transition
 */
class PipWindowBecomesInvisible(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        val matcher = component.get(scenarioInstance)
        flicker.assertWm {
            invoke("hasPipWindow") { it.isPinned(matcher).isAppWindowVisible(matcher) }
                .then()
                .invoke("!hasPipWindow") { it.isNotPinned(matcher).isAppWindowInvisible(matcher) }
        }
    }
}
