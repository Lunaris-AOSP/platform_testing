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
package android.platform.uiautomatorhelpers

import android.os.Build
import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.provider.Settings
import android.util.Log
import org.junit.runners.model.MultipleFailureException

/**
 * Functions to set and restore state around a code-block, using a try-finally. The primary purpose
 * of this utility is to mitigate issues where GestureDetector confuses swipes for long presses due
 * to the severe performance limitations of Cuttlefish devices. This utility is similar to
 * [com.android.launcher3.util.rule.ExtendedLongPressTimeoutRule], but it only applies to CF devices
 * and allows for more fine grained usage.
 *
 * @see com.android.launcher3.util.rule.ExtendedLongPressTimeoutRule
 */
object LongPressTimeoutExtender {

    /**
     * Utility to wrap a lambda in before/after. Basically a try/finally, but with lambdas for
     * before/code block/after. Similar to the @Before/@After annotations but can be used at
     * code-block level granularity.
     *
     * @param before lambda to execute before the code block.
     * @param codeBlock code block to execute.
     * @param after lambda to execute after the code block in a try/finally
     */
    fun doFinally(before: () -> Unit, codeBlock: () -> Unit, after: () -> Unit) {
        val errors = mutableListOf<Throwable>()
        try {
            before()
            codeBlock()
        } catch (t: Throwable) {
            errors.add(t)
        } finally {
            try {
                after()
            } catch (t: Throwable) {
                errors.add(t)
            }
        }
        MultipleFailureException.assertEmpty(errors)
    }

    /**
     * Workaround the hack in GestureDetector which forces a long press if the next move event
     * doesn't arrive before the long press timeout, which seems to happen occaisonally in the
     * emulator.
     *
     * @param codeBlock code block to execute.
     * @see com.android.launcher3.util.rule.ExtendedLongPressTimeoutRule
     */
    fun executeWithExtendedLongPressTimeout(codeBlock: () -> Unit) {
        val longPressMsec = Settings.Secure.getInt(context.contentResolver, "long_press_timeout")

        doFinally(
            {
                if (Build.HARDWARE == CUTTLEFISH_DEVICE) {
                    Log.d(
                        TAG,
                        "set long press timeout to $CUTTLEFISH_LONGPRESS_MSEC, was $longPressMsec",
                    )
                    Settings.Secure.putInt(
                        context.contentResolver,
                        "long_press_timeout",
                        CUTTLEFISH_LONGPRESS_MSEC,
                    )
                }
            },
            { codeBlock() },
            {
                if (Build.HARDWARE == CUTTLEFISH_DEVICE) {
                    Settings.Secure.putInt(
                        context.contentResolver,
                        "long_press_timeout",
                        longPressMsec,
                    )
                    val msg =
                        "restore long press timeout to $longPressMsec, " +
                            "was $CUTTLEFISH_LONGPRESS_MSEC"
                    Log.d(TAG, msg)
                }
            },
        )
    }

    private const val TAG = "StateRestore"

    // Extend the long press timeout to force GestureDetector to recognize swipes
    // on slow (Cuttlefish) devices.
    private const val CUTTLEFISH_LONGPRESS_MSEC = 30000

    // Cuttlefish device Build.HARDWARE string.
    private const val CUTTLEFISH_DEVICE = "cutf_cvm"
}
