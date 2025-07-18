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

package android.tools.flicker.rules

import android.platform.test.rule.ArtifactSaver
import android.tools.FLICKER_TAG
import android.tools.traces.parsers.DeviceDumpParser
import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ArtifactSaverRule : TestWatcher() {
    private var handled = false

    override fun failed(e: Throwable, description: Description?) {
        if (handled) {
            return
        }

        try {
            if (DeviceDumpParser.lastWmTraceData.isNotEmpty()) {
                val fileName = getClassAndMethodName(description) + "lastWmDump.winscope"
                val file = ArtifactSaver.artifactFile(fileName)
                file.writeBytes(DeviceDumpParser.lastWmTraceData)
            }

            if (DeviceDumpParser.lastLayersTraceData.isNotEmpty()) {
                val fileName = getClassAndMethodName(description) + "lastLayersDump.winscope"
                val file = ArtifactSaver.artifactFile(fileName)
                file.writeBytes(DeviceDumpParser.lastLayersTraceData)
            }
        } catch (e: Exception) {
            Log.e(FLICKER_TAG, "Failed to write last Winscope dumps on error", e)
        }

        ArtifactSaver.onError(description, e)

        handled = true
    }

    private fun getClassAndMethodName(description: Description?): String {
        var suffix = description?.methodName
        if (suffix == null) {
            // Can happen when the description is from a ClassRule
            suffix = "EntireClassExecution"
        }
        val testClass = description?.testClass

        // Can have null class if this is a synthetic suite
        val className = if (testClass != null) testClass.simpleName else "SUITE"
        return "$className.$suffix"
    }
}
