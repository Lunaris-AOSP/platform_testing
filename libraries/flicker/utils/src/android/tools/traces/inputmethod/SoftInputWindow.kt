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

package android.tools.traces.inputmethod

import android.graphics.Rect

/**
 * Represents the SoftInputWindowProto in IME traces
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 */
open class SoftInputWindow(val bounds: Rect, val windowState: Int) {
    override fun toString(): String {
        return "${this::class.simpleName}: {windowState: $windowState, bounds: $bounds"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoftInputWindow) return false

        if (bounds != other.bounds) return false
        if (windowState != other.windowState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = windowState
        result = 31 * result + bounds.hashCode()
        return result
    }
}
