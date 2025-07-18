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

package android.tools.datatypes

import android.tools.withCache

/**
 * Wrapper for SizeProto (frameworks/native/services/surfaceflinger/layerproto/common.proto)
 *
 * This class is used by flicker and Winscope
 */
open class Size protected constructor(val width: Int, val height: Int) : DataType() {
    override val isEmpty = height == 0 || width == 0

    override fun doPrintValue() = "$width x $height"

    companion object {
        val EMPTY: Size
            get() = withCache { Size(width = 0, height = 0) }

        @JvmStatic fun from(width: Int, height: Int): Size = withCache { Size(width, height) }
    }
}
