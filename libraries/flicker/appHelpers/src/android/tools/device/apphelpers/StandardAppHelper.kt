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

package android.tools.device.apphelpers

import android.app.ActivityManager
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.tools.PlatformConsts
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.IComponentMatcher
import android.tools.traces.component.IComponentNameMatcher
import android.tools.traces.parsers.WindowManagerStateHelper
import android.tools.withTracing
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.android.launcher3.tapl.LauncherInstrumentation

/**
 * Class to take advantage of {@code IAppHelper} interface so the same test can be run against first
 * party and third party apps.
 */
open class StandardAppHelper(
    val instrumentation: Instrumentation,
    val appName: String,
    val componentMatcher: IComponentNameMatcher,
) : IStandardAppHelper, IComponentNameMatcher by componentMatcher {
    constructor(
        instr: Instrumentation,
        appName: String,
        packageName: String,
        activity: String,
    ) : this(instr, appName, ComponentNameMatcher(packageName, ".$activity"))

    protected val pkgManager: PackageManager = instrumentation.context.packageManager

    protected val tapl: LauncherInstrumentation by lazy { LauncherInstrumentation() }

    private val activityManager: ActivityManager?
        get() = instrumentation.context.getSystemService(ActivityManager::class.java)

    protected val context: Context
        get() = instrumentation.context

    override val packageName = componentMatcher.packageName

    override val className = componentMatcher.className

    protected val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)

    private fun getAppSelector(expectedPackageName: String): BySelector {
        val expected = expectedPackageName.ifEmpty { packageName }
        return By.pkg(expected).depth(0)
    }

    override fun open() {
        open(packageName)
    }

    protected fun open(expectedPackageName: String) {
        tapl.goHome().switchToAllApps().getAppIcon(appName).launch(expectedPackageName)
    }

    /** {@inheritDoc} */
    open val openAppIntent: Intent
        get() {
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.component = ComponentName(packageName, className)
            return intent
        }

    /** {@inheritDoc} */
    override fun exit() {
        withTracing("${this::class.simpleName}#exit") {
            // Ensure all testing components end up being closed.
            activityManager?.forceStopPackage(packageName)
        }
    }

    /** {@inheritDoc} */
    override fun exit(wmHelper: WindowManagerStateHelper) {
        withTracing("${this::class.simpleName}#exitAndWait") {
            exit()
            waitForActivityDestroyed(wmHelper)
        }
    }

    /** Waits the activity until state change to {link WindowManagerState.STATE_DESTROYED} */
    private fun waitForActivityDestroyed(wmHelper: WindowManagerStateHelper) {
        val waitMsg =
            "state of ${componentMatcher.toActivityIdentifier()} to be " +
                PlatformConsts.STATE_DESTROYED
        wmHelper
            .StateSyncBuilder()
            .add(waitMsg) {
                !it.wmState.containsActivity(componentMatcher) ||
                    it.wmState.hasActivityState(componentMatcher, PlatformConsts.STATE_DESTROYED)
            }
            .withAppTransitionIdle()
            .waitForAndVerify()
    }

    private fun launchAppViaIntent(
        action: String? = null,
        stringExtras: Map<String, String> = mapOf(),
    ) {
        withTracing("${this::class.simpleName}#launchAppViaIntent") {
            val intent = openAppIntent
            intent.action = action ?: Intent.ACTION_MAIN
            stringExtras.forEach { intent.putExtra(it.key, it.value) }
            context.startActivity(intent)
        }
    }

    /** {@inheritDoc} */
    override fun launchViaIntent(
        expectedPackageName: String,
        action: String?,
        stringExtras: Map<String, String>,
    ) {
        launchAppViaIntent(action, stringExtras)
        val appSelector = getAppSelector(expectedPackageName)
        uiDevice.wait(Until.hasObject(appSelector), APP_LAUNCH_WAIT_TIME_MS)
    }

    /** {@inheritDoc} */
    override fun launchViaIntent(
        wmHelper: WindowManagerStateHelper,
        launchedAppComponentMatcherOverride: IComponentMatcher?,
        action: String?,
        stringExtras: Map<String, String>,
        waitConditionsBuilder: WindowManagerStateHelper.StateSyncBuilder,
    ) {
        launchAppViaIntent(action, stringExtras)
        doWaitShown(launchedAppComponentMatcherOverride, waitConditionsBuilder)
    }

    /** {@inheritDoc} */
    override fun launchViaIntent(
        wmHelper: WindowManagerStateHelper,
        intent: Intent,
        launchedAppComponentMatcherOverride: IComponentMatcher?,
        waitConditionsBuilder: WindowManagerStateHelper.StateSyncBuilder,
    ) {
        withTracing("${this::class.simpleName}#launchViaIntent") {
            context.startActivity(intent)
            doWaitShown(launchedAppComponentMatcherOverride, waitConditionsBuilder)
        }
    }

    private fun doWaitShown(
        launchedAppComponentMatcherOverride: IComponentMatcher? = null,
        waitConditionsBuilder: WindowManagerStateHelper.StateSyncBuilder,
    ) {
        withTracing("${this::class.simpleName}#doWaitShown") {
            val expectedWindow = launchedAppComponentMatcherOverride ?: componentMatcher
            doWaitShownLight(expectedWindow)
            doWaitShownHeavy(expectedWindow, waitConditionsBuilder)
        }
    }

    private fun doWaitShownLight(expectedWindow: IComponentMatcher) {
        try {
            val expectedPackageName =
                ComponentNameMatcher.unflattenFromString(expectedWindow.toWindowIdentifier())
                    .packageName
            val appSelector = getAppSelector(expectedPackageName)
            uiDevice.wait(Until.hasObject(appSelector), APP_LAUNCH_WAIT_TIME_MS)
        } catch (e: Exception) {
            // IComponentMatcher#toWindowIdentifier() might not be implemented.
            // Let's just skip the light-weight busy waiting.
        }
    }

    private fun doWaitShownHeavy(
        expectedWindow: IComponentMatcher,
        waitConditionsBuilder: WindowManagerStateHelper.StateSyncBuilder,
    ) {
        val builder = waitConditionsBuilder.withWindowSurfaceAppeared(expectedWindow)
        builder.waitForAndVerify()
    }

    override fun isAvailable(): Boolean {
        return try {
            pkgManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun toString(): String = componentMatcher.toString()

    companion object {
        private const val APP_LAUNCH_WAIT_TIME_MS = 10000L
    }
}
