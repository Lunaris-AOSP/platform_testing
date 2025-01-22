/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.tools.parsers.wm

import android.tools.Cache
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.readAsset
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.TransitionsTraceParser
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

/** Tests for [TransitionsTraceParser] */
class TransitionsTraceParserTest {
    @Before
    fun before() {
        Assume.assumeTrue(android.tracing.Flags.perfettoTransitionTracing())
        Cache.clear()
    }

    @Test
    fun canParse() {
        TraceProcessorSession.loadPerfettoTrace(readAsset("transitions.perfetto-trace")) { session
            ->
            val trace = TransitionsTraceParser().parse(session)
            Truth.assertWithMessage("Unable to parse dump").that(trace.entries).hasSize(4)
        }
    }

    @Test
    fun canParseTimestamps() {
        TraceProcessorSession.loadPerfettoTrace(readAsset("transitions.perfetto-trace")) { session
            ->
            val trace = TransitionsTraceParser().parse(session)
            Truth.assertWithMessage("Has all timestamps")
                .that(trace.entries.first().timestamp.hasAllTimestamps)
                .isTrue()
            Truth.assertWithMessage("Has unix timestamps")
                .that(trace.entries.first().timestamp.unixNanos)
                .isEqualTo(1700573425428925648L)
            Truth.assertWithMessage("Has elapsed timestamps")
                .that(trace.entries.first().timestamp.elapsedNanos)
                .isEqualTo(479583450794L)
            Truth.assertWithMessage("Has uptime timestamps")
                .that(trace.entries.first().timestamp.systemUptimeNanos)
                .isEqualTo(479583450997L)
        }
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
