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

package android.tools.helpers

import android.graphics.Rect
import android.graphics.Region
import android.tools.PlatformConsts
import android.tools.Rotation
import android.tools.traces.getCurrentStateDump
import android.tools.traces.surfaceflinger.Display
import android.tools.traces.wm.DisplayContent
import android.tools.traces.wm.InsetsSource
import android.util.LruCache
import android.view.WindowInsets
import androidx.test.platform.app.InstrumentationRegistry

object WindowUtils {

    private val displayBoundsCache = LruCache<Rotation, Rect>(4)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /** Helper functions to retrieve system window sizes and positions. */
    private val context by lazy { instrumentation.context }

    private val resources
        get() = context.resources

    /** Get the display bounds */
    val displayBounds: Rect
        get() {
            val currState = getCurrentStateDump(clearCacheAfterParsing = false)
            return currState.layerState.physicalDisplay?.layerStackSpace ?: Rect()
        }

    val displayStableBounds: Rect
        get() {
            val currState = getCurrentStateDump(clearCacheAfterParsing = false)
            return currState.wmState.getDefaultDisplay()?.stableBounds ?: Rect()
        }

    /** Gets the current display rotation */
    val displayRotation: Rotation
        get() {
            val currState = getCurrentStateDump(clearCacheAfterParsing = false)
            return currState.wmState.getRotation(PlatformConsts.DEFAULT_DISPLAY)
        }

    /** Gets the default display ID. */
    val defaultDisplayId: Int
        get() =
            getCurrentStateDump(clearCacheAfterParsing = false)
                .wmState
                .getDefaultDisplay()
                ?.displayId ?: error("Missing physical display")

    /**
     * Get the display bounds when the device is at a specific rotation
     *
     * @param requestedRotation Device rotation
     */
    @JvmStatic
    fun getDisplayBounds(requestedRotation: Rotation): Rect {
        return displayBoundsCache[requestedRotation]
            ?: let {
                val displayIsRotated = displayRotation.isRotated()
                val requestedDisplayIsRotated = requestedRotation.isRotated()

                // if the current orientation changes with the requested rotation,
                // flip height and width of display bounds.
                val displayBounds = displayBounds
                val retval =
                    if (displayIsRotated != requestedDisplayIsRotated) {
                        Rect(0, 0, displayBounds.height(), displayBounds.width())
                    } else {
                        Rect(0, 0, displayBounds.width(), displayBounds.height())
                    }
                displayBoundsCache.put(requestedRotation, retval)
                return retval
            }
    }

    @JvmStatic
    fun getInsetDisplayBounds(requestedRotation: Rotation): Rect {
        val currState = getCurrentStateDump(clearCacheAfterParsing = false)
        val display = currState.wmState.getDefaultDisplay() ?: error("Missing physical display")

        // check device is rotated, and if so, rotate the returned inset bounds
        val insetDisplayBounds =
            with(display.displayRect) {
                if (displayRotation.isRotated() == requestedRotation.isRotated()) {
                    Rect(left, top, right, bottom)
                } else {
                    Rect(left, top, bottom, right)
                }
            }

        // Find visible insets from status bar and navigation bar (equivalent to taskbar)
        display.insetsSourceProviders.forEach {
            val insetsSource: InsetsSource = it.source ?: return@forEach
            val insets: Rect = it.frame ?: return@forEach
            if (!insetsSource.visible) return@forEach

            when (insetsSource.type) {
                // Returned insets are based on the display bounds in its natural orientation,
                // so we calculate the delta between the insets and display bounds when not rotated,
                // then apply it to the properly rotated (if necessary) display bounds
                WindowInsets.Type.statusBars() -> {
                    val topDelta = insets.bottom - display.displayRect.top
                    insetDisplayBounds.top += topDelta
                }
                WindowInsets.Type.navigationBars() -> {
                    val botDelta = display.displayRect.bottom - insets.top
                    insetDisplayBounds.bottom -= botDelta
                }
            }
        }

        return insetDisplayBounds
    }

    /** Gets the status bar height with a specific display cutout. */
    private fun getExpectedStatusBarHeight(displayContent: DisplayContent): Int {
        val cutout = displayContent.cutout
        val defaultSize = status_bar_height_default
        val safeInsetTop = cutout?.insets?.top ?: 0
        val waterfallInsetTop = cutout?.waterfallInsets?.top ?: 0
        // The status bar height should be:
        // Max(top cutout size, (status bar default height + waterfall top size))
        return safeInsetTop.coerceAtLeast(defaultSize + waterfallInsetTop)
    }

    /**
     * Gets the expected status bar position for a specific display
     *
     * @param display the main display
     */
    @JvmStatic
    fun getExpectedStatusBarPosition(display: DisplayContent): Region {
        val height = getExpectedStatusBarHeight(display)
        return Region(0, 0, display.displayRect.width(), height)
    }

    /**
     * Gets the expected navigation bar position for a specific display
     *
     * @param display the main display
     */
    @JvmStatic
    fun getNavigationBarPosition(display: Display): Region {
        return getNavigationBarPosition(display, isGesturalNavigationEnabled)
    }

    /**
     * Gets the expected navigation bar position for a specific display
     *
     * @param display the main display
     * @param isGesturalNavigation whether gestural navigation is enabled
     */
    @JvmStatic
    fun getNavigationBarPosition(display: Display, isGesturalNavigation: Boolean): Region {
        val navBarWidth = getDimensionPixelSize("navigation_bar_width")
        val displayHeight = display.layerStackSpace.height()
        val displayWidth = display.layerStackSpace.width()
        val requestedRotation = display.transform.getRotation()
        val navBarHeight = getNavigationBarFrameHeight(requestedRotation, isGesturalNavigation)

        return when {
            // nav bar is at the bottom of the screen
            !requestedRotation.isRotated() || isGesturalNavigation ->
                Region(0, displayHeight - navBarHeight, displayWidth, displayHeight)
            // nav bar is on the right side
            requestedRotation == Rotation.ROTATION_90 ->
                Region(displayWidth - navBarWidth, 0, displayWidth, displayHeight)
            // nav bar is on the left side
            requestedRotation == Rotation.ROTATION_270 -> Region(0, 0, navBarWidth, displayHeight)
            else -> error("Unknown rotation $requestedRotation")
        }
    }

    /**
     * Estimate the navigation bar position at a specific rotation
     *
     * @param requestedRotation Device rotation
     */
    @JvmStatic
    fun estimateNavigationBarPosition(requestedRotation: Rotation): Region {
        val displayBounds = displayBounds
        val displayWidth: Int
        val displayHeight: Int
        if (!requestedRotation.isRotated()) {
            displayWidth = displayBounds.width()
            displayHeight = displayBounds.height()
        } else {
            // swap display dimensions in landscape or seascape mode
            displayWidth = displayBounds.height()
            displayHeight = displayBounds.width()
        }
        val navBarWidth = getDimensionPixelSize("navigation_bar_width")
        val navBarHeight =
            getNavigationBarFrameHeight(requestedRotation, isGesturalNavigation = false)

        return when {
            // nav bar is at the bottom of the screen
            !requestedRotation.isRotated() || isGesturalNavigationEnabled ->
                Region(0, displayHeight - navBarHeight, displayWidth, displayHeight)
            // nav bar is on the right side
            requestedRotation == Rotation.ROTATION_90 ->
                Region(displayWidth - navBarWidth, 0, displayWidth, displayHeight)
            // nav bar is on the left side
            requestedRotation == Rotation.ROTATION_270 -> Region(0, 0, navBarWidth, displayHeight)
            else -> error("Unknown rotation $requestedRotation")
        }
    }

    /** Checks if the device uses gestural navigation */
    val isGesturalNavigationEnabled: Boolean
        get() {
            val resourceId =
                resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
            return resources.getInteger(resourceId) == 2
        }

    @JvmStatic
    fun getDimensionPixelSize(resourceName: String): Int {
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    /** Gets the navigation bar frame height */
    @JvmStatic
    fun getNavigationBarFrameHeight(rotation: Rotation, isGesturalNavigation: Boolean): Int {
        return if (rotation.isRotated()) {
            if (isGesturalNavigation) {
                getDimensionPixelSize("navigation_bar_frame_height")
            } else {
                getDimensionPixelSize("navigation_bar_height_landscape")
            }
        } else {
            getDimensionPixelSize("navigation_bar_frame_height")
        }
    }

    private val status_bar_height_default: Int
        get() {
            val resourceId =
                resources.getIdentifier("status_bar_height_default", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }

    val quick_qs_offset_height: Int
        get() {
            val resourceId = resources.getIdentifier("quick_qs_offset_height", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }

    /** Split screen divider inset height */
    val dockedStackDividerInset: Int
        get() {
            val resourceId =
                resources.getIdentifier("docked_stack_divider_insets", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }
}
