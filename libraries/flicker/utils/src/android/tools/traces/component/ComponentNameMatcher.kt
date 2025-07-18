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

package android.tools.traces.component

import android.tools.traces.surfaceflinger.Layer
import android.tools.traces.wm.Activity
import android.tools.traces.wm.WindowContainer
import java.util.function.Predicate

/** ComponentMatcher based on name */
class ComponentNameMatcher(var component: ComponentName) : IComponentNameMatcher {
    override val packageName: String
        get() = component.packageName

    override val className: String
        get() = component.className

    override fun toActivityName(): String = component.toActivityName()

    override fun toWindowName(): String = component.toWindowName()

    override fun toLayerName(): String = component.toLayerName()

    constructor(
        packageName: String,
        className: String,
    ) : this(ComponentName(packageName, className))

    constructor(className: String) : this("", className)

    override fun activityRecordMatchesAnyOf(layers: Collection<Layer>): Boolean =
        layers.any { activityRecordFilter.invoke(it.name) }

    override fun componentNameMatcherToString(): String {
        return "ComponentNameMatcher(\"${this.packageName}\", " + "\"${this.className}\")"
    }

    /** {@inheritDoc} */
    override fun windowMatchesAnyOf(windows: Collection<WindowContainer>): Boolean =
        windows.any { windowNameFilter.invoke(it.title) }

    /** {@inheritDoc} */
    override fun activityMatchesAnyOf(activities: Collection<Activity>): Boolean =
        activities.any { activityNameFilter.invoke(it.name) }

    /** {@inheritDoc} */
    override fun layerMatchesAnyOf(layers: Collection<Layer>): Boolean =
        layers.any { layerNameFilter.invoke(it.name) }

    /** {@inheritDoc} */
    override fun check(
        layers: Collection<Layer>,
        condition: Predicate<Collection<Layer>>,
    ): Boolean = condition.test(layers.filter { layerMatchesAnyOf(it) })

    /** {@inheritDoc} */
    override fun toActivityIdentifier(): String = component.toActivityName()

    /** {@inheritDoc} */
    override fun toWindowIdentifier(): String = component.toWindowName()

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String = component.toLayerName()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentNameMatcher) return false
        return component == other.component
    }

    override fun hashCode(): Int = component.hashCode()

    override fun toString(): String = component.toString()

    private val activityRecordFilter: (String) -> Boolean
        get() = { it.startsWith("ActivityRecord{") && it.contains(component.toShortWindowName()) }

    private val activityNameFilter: (String) -> Boolean
        get() = { it.contains(component.toActivityName()) }

    private val windowNameFilter: (String) -> Boolean
        get() = { it.contains(component.toWindowName()) }

    private val layerNameFilter: (String) -> Boolean
        get() = { it.contains(component.toLayerName()) }

    companion object {
        @JvmField val NAV_BAR = ComponentNameMatcher("", "NavigationBar0")
        @JvmField val TASK_BAR = ComponentNameMatcher("", "Taskbar")
        @JvmField val STATUS_BAR = ComponentNameMatcher("", "StatusBar")
        @JvmField val ROTATION = ComponentNameMatcher("", "RotationLayer")
        @JvmField val BACK_SURFACE = ComponentNameMatcher("", "BackColorSurface")
        @JvmField val IME = ComponentNameMatcher("", "InputMethod")
        @JvmField val IME_SNAPSHOT = ComponentNameMatcher("", "IME-snapshot-surface")
        @JvmField val SPLASH_SCREEN = ComponentNameMatcher("", "Splash Screen")
        @JvmField val SNAPSHOT = ComponentNameMatcher("", "SnapshotStartingWindow")
        @JvmField val SECONDARY_HOME_HANDLE = ComponentNameMatcher("", "SecondaryHomeHandle")

        @JvmField val TRANSITION_SNAPSHOT = ComponentNameMatcher("", "transition snapshot")
        @JvmField val LETTERBOX = ComponentNameMatcher("", "Letterbox")

        @JvmField val WALLPAPER_BBQ_WRAPPER = ComponentNameMatcher("", "Wallpaper BBQ wrapper")

        @JvmField val PIP_CONTENT_OVERLAY = ComponentNameMatcher("", "PipContentOverlay")

        @JvmField val PIP_MENU_OVERLAY = ComponentNameMatcher("", "PipMenuView")

        @JvmField
        val EDGE_BACK_GESTURE_HANDLER = ComponentNameMatcher("", "EdgeBackGestureHandler0")

        @JvmField val COLOR_FADE = ComponentNameMatcher("", "ColorFade")

        @JvmField val WALLPAPER_WINDOW_TOKEN = ComponentNameMatcher("", "WallpaperWindowToken")

        @JvmField val NOTIFICATION_SHADE = ComponentNameMatcher("", "NotificationShade")

        @JvmField val VOLUME_DIALOG = ComponentNameMatcher("", "VolumeDialog")

        @JvmField val FLOATING_ROTATION_BUTTON = ComponentNameMatcher("", "FloatingRotationButton")

        @JvmField val SCREEN_DECOR_OVERLAY = ComponentNameMatcher("", "ScreenDecorHwcOverlay#")

        @JvmField val POINTER_LOCATION = ComponentNameMatcher("", "PointerLocation")

        @JvmField
        val WIRED_CHARGING_ANIMATION = ComponentNameMatcher("", "Wired Charging Animation#")

        @JvmField val SCREEN_RECORDING_OVERLAYS = SCREEN_DECOR_OVERLAY.or(POINTER_LOCATION)

        @JvmField
        val LAUNCHER =
            ComponentNameMatcher(
                "com.google.android.apps.nexuslauncher",
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity",
            )

        @JvmField
        val AOSP_LAUNCHER =
            ComponentNameMatcher(
                "com.android.launcher3",
                "com.android.launcher3.uioverrides.QuickstepLauncher",
            )

        @JvmField val SPLIT_DIVIDER = ComponentNameMatcher("", "StageCoordinatorSplitDivider")

        @JvmField val DEFAULT_TASK_DISPLAY_AREA = ComponentNameMatcher("", "DefaultTaskDisplayArea")

        /**
         * Creates a component matcher from a window or layer name.
         *
         * Requires the [str] to contain both the package and class name (with a / separator)
         *
         * @param str Value to parse
         */
        @JvmStatic
        fun unflattenFromString(str: String): ComponentNameMatcher {
            val sep = str.indexOf('/')
            if (sep < 0 || sep + 1 >= str.length) {
                error("Missing package/class separator")
            }
            val pkg = str.substring(0, sep)
            var cls = str.substring(sep + 1)
            if (cls.isNotEmpty() && cls[0] == '.') {
                cls = pkg + cls
            }
            return ComponentNameMatcher(pkg, cls)
        }

        /**
         * Creates a component matcher from a window or layer name. The name might contain junk,
         * which will be removed to only extract package and class name (e.g. other words before
         * package name, separated by spaces, #id in the end after the class name)
         *
         * Requires the [str] to contain both the package and class name (with a / separator)
         *
         * @param str Value to parse
         */
        @JvmStatic
        fun unflattenFromStringWithJunk(str: String): ComponentNameMatcher {
            val sep = str.indexOf('/')
            if (sep < 0 || sep + 1 >= str.length) {
                error("Missing package/class separator")
            }

            var pkg = str.substring(0, sep)
            var pkgSep: Int = -1
            val pkgCharArr = pkg.toCharArray()
            for (index in (0..pkgCharArr.lastIndex).reversed()) {
                val currentChar = pkgCharArr[index]
                if (currentChar !in 'A'..'Z' && currentChar !in 'a'..'z' && currentChar != '.') {
                    pkgSep = index
                    break
                }
            }
            if (!(pkgSep < 0 || pkgSep + 1 >= pkg.length)) {
                pkg = pkg.substring(pkgSep, pkg.length)
            }

            var cls = str.substring(sep + 1)
            var clsSep = -1 // cls.indexOf('#')
            val clsCharArr = cls.toCharArray()
            for (index in (0..clsCharArr.lastIndex)) {
                val currentChar = clsCharArr[index]
                if (currentChar !in 'A'..'Z' && currentChar !in 'a'..'z' && currentChar != '.') {
                    clsSep = index
                    break
                }
            }
            if (!(clsSep < 0 || clsSep + 1 >= cls.length)) {
                cls = cls.substring(0, clsSep)
            }

            if (cls.isNotEmpty() && cls[0] == '.') {
                cls = pkg + cls
            }
            return ComponentNameMatcher(pkg, cls)
        }
    }
}
