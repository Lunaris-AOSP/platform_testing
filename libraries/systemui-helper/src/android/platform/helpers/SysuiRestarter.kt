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
package android.platform.helpers

import android.platform.helpers.CommonUtils.assertScreenOn
import android.platform.helpers.Constants.UI_PACKAGE_NAME_SYSUI
import android.platform.helpers.LockscreenUtils.LockscreenType
import android.platform.helpers.features.common.HomeLockscreenPage
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DurationUtils.platformAdjust
import androidx.test.uiautomator.By
import com.android.app.tracing.traceSection
import com.android.systemui.Flags
import java.time.Duration

/** Restarts system ui. */
object SysuiRestarter {

    private val sysuiProcessUtils = ProcessUtil(UI_PACKAGE_NAME_SYSUI)

    val LOCKSCREEN_SELECTOR =
        if (Flags.sceneContainer()) {
            By.res("element:lockscreen")
        } else {
            By.res("com.android.systemui", "keyguard_indication_area")
        }

    /**
     * Restart System UI by running `am crash com.android.systemui`.
     *
     * This is sometimes necessary after changing flags, configs, or settings ensure that systemui
     * is properly initialized with the new changes. This method will wait until the home screen is
     * visible, then it will optionally dismiss the home screen via swipe.
     *
     * @param swipeUp whether to call [HomeLockscreenPage.swipeUp] after restarting System UI
     */
    @JvmStatic
    fun restartSystemUI(swipeUp: Boolean) {
        traceSection("restartSystemUI") {
            // This method assumes the screen is on.
            assertScreenOn("restartSystemUI needs the screen to be on.")
            // make sure the lock screen is enable.
            LockscreenUtils.setLockscreen(
                LockscreenType.SWIPE,
                /* lockscreenCode= */ null,
                /* expectedResult= */ false,
            )
            sysuiProcessUtils.restart()
            assertLockscreenVisibility(true) { "Lockscreen not visible after restart" }
            if (swipeUp) {
                HomeLockscreenPage().swipeUp()
                assertLockscreenVisibility(false) { "Lockscreen still visible after swiping up." }
            }
        }
    }

    private fun assertLockscreenVisibility(visible: Boolean, errorMessageProvider: () -> String) {
        uiDevice.assertVisibility(
            LOCKSCREEN_SELECTOR,
            visible,
            timeout = Duration.ofSeconds(10).platformAdjust(),
            errorProvider = errorMessageProvider,
        )
    }
}
