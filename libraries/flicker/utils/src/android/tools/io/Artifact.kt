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

package android.tools.io

interface Artifact {
    val runStatus: RunStatus
    val absolutePath: String
    val fileName: String

    /** Stable identifier for this artifact (e.g. scenario + counter without current status) */
    val stableId: String

    fun updateStatus(newStatus: RunStatus)

    /* reads the entire artifact */
    fun readBytes(): ByteArray

    fun readBytes(descriptor: ResultArtifactDescriptor): ByteArray?

    /** @return if a file matching [descriptor exists in the artifact */
    fun hasTrace(descriptor: ResultArtifactDescriptor): Boolean

    /** @return the number of files in the artifact */
    fun traceCount(): Int

    fun deleteIfExists()
}
