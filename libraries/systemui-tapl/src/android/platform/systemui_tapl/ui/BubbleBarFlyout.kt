/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.systemui_tapl.ui

import android.platform.systemui_tapl.utils.DeviceUtils.launcherResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click

/**
 * Provides an API for interacting with the bubble bar flyout within launcher in UI automation
 * tests.
 */
class BubbleBarFlyout {

    init {
        BUBBLE_BAR_FLYOUT_VIEW.assertVisible()
    }

    /** Taps on the bubble bar flyout to expand it into the expanded bubble. */
    fun expand() {
        BUBBLE_BAR_FLYOUT_VIEW.click()
        BUBBLE_BAR_FLYOUT_VIEW.assertInvisible()
    }

    companion object {
        internal val BUBBLE_BAR_FLYOUT_VIEW = launcherResSelector("bubble_bar_flyout_view")
    }
}
