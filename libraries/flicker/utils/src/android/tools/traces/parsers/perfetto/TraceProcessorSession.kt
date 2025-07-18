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

@file:OptIn(
    androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi::class,
    androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi::class,
)

package android.tools.traces.parsers.perfetto

import android.tools.io.TraceType
import android.tools.withTracing
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.macro.runServer
import java.io.File
import java.io.FileOutputStream

typealias Row = Map<String, Any?>

class TraceProcessorSession(val session: TraceProcessor.Session) {

    fun <T> query(sql: String, predicate: (List<Row>) -> T): T {
        return withTracing("TraceProcessorSession#query") {
            val rows = session.query(sql)
            predicate(rows.toList())
        }
    }

    companion object {
        @JvmStatic
        fun <T> loadPerfettoTrace(trace: ByteArray, predicate: (TraceProcessorSession) -> T): T {
            return withTracing("TraceProcessorSession#loadPerfettoTrace") {
                val traceFile = File.createTempFile(TraceType.SF.fileName, "")
                FileOutputStream(traceFile).use { it.write(trace) }
                val result =
                    TraceProcessor.runServer {
                        loadTrace(PerfettoTrace(traceFile.absolutePath)) {
                            predicate(TraceProcessorSession(this))
                        }
                    }
                result
            }
        }
    }
}