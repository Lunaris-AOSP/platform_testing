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

package android.tools.flicker.extractors

import android.tools.Timestamp
import android.tools.traces.events.ICujType
import android.tools.traces.wm.Transition

data class TraceSlice(
    val startTimestamp: Timestamp,
    val endTimestamp: Timestamp,
    val associatedTransition: Transition? = null,
    val associatedCuj: ICujType? = null,
) {
    init {
        require(startTimestamp.hasAllTimestamps) {
            "startTimestamp ($startTimestamp) has missing timestamps"
        }
        require(endTimestamp.hasAllTimestamps) {
            "endTimestamp ($endTimestamp) has missing timestamps"
        }
    }
}
