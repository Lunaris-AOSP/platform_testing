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

@file:JvmName("Utils")

package android.tools.traces

import android.app.UiAutomation
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.tools.MILLISECOND_AS_NANOSECONDS
import android.tools.io.TraceType
import android.tools.traces.io.ResultReader
import android.tools.traces.monitors.PerfettoTraceMonitor
import android.tools.traces.parsers.DeviceDumpParser
import android.tools.traces.surfaceflinger.LayerTraceEntry
import android.tools.traces.wm.WindowManagerState
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.protobuf.InvalidProtocolBufferException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Optional
import java.util.TimeZone
import perfetto.protos.PerfettoConfig.TracingServiceState

fun formatRealTimestamp(timestampNs: Long): String {
    val timestampMs = timestampNs / MILLISECOND_AS_NANOSECONDS
    val remainderNs = timestampNs % MILLISECOND_AS_NANOSECONDS
    val date = Date(timestampMs)

    val timeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH)
    timeFormatter.timeZone = TimeZone.getTimeZone("UTC")

    return "${timeFormatter.format(date)}${remainderNs.toString().padStart(6, '0')}"
}

fun executeShellCommand(cmd: String): ByteArray {
    Log.d(LOG_TAG, "Executing shell command $cmd")
    val uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val fileDescriptor = uiAutomation.executeShellCommand(cmd)
    ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor).use { inputStream ->
        return inputStream.readBytes()
    }
}

fun executeShellCommand(cmd: String, stdin: ByteArray): ByteArray {
    Log.d(LOG_TAG, "Executing shell command $cmd")
    val uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val fileDescriptors = uiAutomation.executeShellCommandRw(cmd)
    val stdoutFileDescriptor = fileDescriptors[0]
    val stdinFileDescriptor = fileDescriptors[1]

    ParcelFileDescriptor.AutoCloseOutputStream(stdinFileDescriptor).use { it.write(stdin) }

    ParcelFileDescriptor.AutoCloseInputStream(stdoutFileDescriptor).use {
        return it.readBytes()
    }
}

private fun doBinderDump(name: String): ByteArray {
    // create a fd for the binder transaction
    val pipe = ParcelFileDescriptor.createPipe()
    val source = pipe[0]
    val sink = pipe[1]

    // ServiceManager isn't accessible from tests, so use reflection
    // this should return an IBinder
    val service =
        Class.forName("android.os.ServiceManager")
            .getMethod("getServiceOrThrow", String::class.java)
            .invoke(null, name) as IBinder?

    // this is equal to ServiceManager::PROTO_ARG
    val args = arrayOf("--proto")
    service?.dump(sink.fileDescriptor, args)
    sink.close()

    // convert the FD into a ByteArray
    ParcelFileDescriptor.AutoCloseInputStream(source).use { inputStream ->
        return inputStream.readBytes()
    }
}

private fun getCurrentWindowManagerState() = doBinderDump("window")

/**
 * Gets the current device state dump containing the [WindowManagerState] (optional) and the
 * [LayerTraceEntry] (optional) in raw (byte) data.
 *
 * @param dumpTypes Flags determining which types of traces should be included in the dump
 */
fun getCurrentState(
    vararg dumpTypes: TraceType = arrayOf(TraceType.SF_DUMP, TraceType.WM_DUMP)
): Pair<ByteArray, ByteArray> {
    if (dumpTypes.isEmpty()) {
        throw IllegalArgumentException("No dump specified")
    }

    val traceTypes = dumpTypes.filter { it.isTrace }
    if (traceTypes.isNotEmpty()) {
        throw IllegalArgumentException("Only dump types are supported. Invalid types: $traceTypes")
    }

    val requestedWmDump = dumpTypes.contains(TraceType.WM_DUMP)
    val requestedSfDump = dumpTypes.contains(TraceType.SF_DUMP)

    Log.d(LOG_TAG, "Requesting new device state dump")

    val reader =
        PerfettoTraceMonitor.newBuilder()
            .also {
                if (requestedWmDump && android.tracing.Flags.perfettoWmDump()) {
                    it.enableWindowManagerDump()
                }

                if (requestedSfDump) {
                    it.enableLayersDump()
                }
            }
            .build()
            .withTracing(resultReaderProvider = { ResultReader(it, SERVICE_TRACE_CONFIG) }) {}
    val perfettoTrace = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)

    reader.artifact.deleteIfExists()

    val wmDump =
        if (android.tracing.Flags.perfettoWmDump()) {
            if (requestedWmDump) perfettoTrace else ByteArray(0)
        } else {
            if (requestedWmDump) {
                Log.d(LOG_TAG, "Requesting new legacy WM state dump")
                getCurrentWindowManagerState()
            } else {
                ByteArray(0)
            }
        }

    val sfDump = if (requestedSfDump) perfettoTrace else ByteArray(0)

    return Pair(wmDump, sfDump)
}

/**
 * Gets the current device state dump containing the [WindowManagerState] (optional) and the
 * [LayerTraceEntry] (optional) parsed
 *
 * @param dumpTypes Flags determining which types of traces should be included in the dump
 * @param clearCacheAfterParsing If the caching used while parsing the proto should be
 *
 * ```
 *                               cleared or remain in memory
 * ```
 */
@JvmOverloads
fun getCurrentStateDumpNullable(
    vararg dumpTypes: TraceType = arrayOf(TraceType.SF_DUMP, TraceType.WM_DUMP),
    clearCacheAfterParsing: Boolean = true,
): NullableDeviceStateDump {
    val currentStateDump = getCurrentState(*dumpTypes)
    return DeviceDumpParser.fromNullableDump(
        currentStateDump.first,
        currentStateDump.second,
        clearCacheAfterParsing = clearCacheAfterParsing,
    )
}

@JvmOverloads
fun getCurrentStateDump(
    vararg dumpTypes: TraceType = arrayOf(TraceType.SF_DUMP, TraceType.WM_DUMP),
    clearCacheAfterParsing: Boolean = true,
): DeviceStateDump {
    val currentStateDump = getCurrentState(*dumpTypes)
    return DeviceDumpParser.fromDump(
        currentStateDump.first,
        currentStateDump.second,
        clearCacheAfterParsing = clearCacheAfterParsing,
    )
}

@JvmOverloads
fun busyWaitForDataSourceRegistration(
    dataSourceName: String,
    busyWaitIntervalMs: Long = 100,
    timeoutMs: Long = 10000,
) {
    busyWait(
        busyWaitIntervalMs,
        timeoutMs,
        { isDataSourceAvailable(dataSourceName) },
        { "Data source disn't  become available" },
    )
}

@JvmOverloads
fun busyWaitTracingSessionExists(
    uniqueSessionName: String,
    busyWaitIntervalMs: Long = 100,
    timeoutMs: Long = 10000,
) {
    busyWait(
        busyWaitIntervalMs,
        timeoutMs,
        { sessionExists(uniqueSessionName) },
        { "Tracing session doesn't exist" },
    )
}

@JvmOverloads
fun busyWaitTracingSessionDoesntExist(
    uniqueSessionName: String,
    busyWaitIntervalMs: Long = 100,
    timeoutMs: Long = 10000,
) {
    busyWait(
        busyWaitIntervalMs,
        timeoutMs,
        { !sessionExists(uniqueSessionName) },
        { "Tracing session still exists" },
    )
}

private fun isDataSourceAvailable(dataSourceName: String): Boolean {
    val proto = executeShellCommand("perfetto --query-raw")

    try {
        val state = TracingServiceState.parseFrom(proto)

        var producerId = Optional.empty<Int>()

        for (producer in state.producersList) {
            if (producer.pid == Process.myPid()) {
                producerId = Optional.of(producer.id)
                break
            }
        }

        if (!producerId.isPresent) {
            return false
        }

        for (ds in state.dataSourcesList) {
            if (ds.dsDescriptor.name.equals(dataSourceName) && ds.producerId == producerId.get()) {
                return true
            }
        }
    } catch (e: InvalidProtocolBufferException) {
        throw RuntimeException(e)
    }

    return false
}

private fun sessionExists(uniqueSessionName: String): Boolean {
    val proto = executeShellCommand("perfetto --query-raw")

    try {
        val state = TracingServiceState.parseFrom(proto)

        for (session in state.tracingSessionsList) {
            if (session.uniqueSessionName.equals(uniqueSessionName)) {
                return true
            }
        }
    } catch (e: InvalidProtocolBufferException) {
        throw RuntimeException(e)
    }

    return false
}

private fun busyWait(
    busyWaitIntervalMs: Long,
    timeoutMs: Long,
    predicate: () -> Boolean,
    errorMessage: () -> String,
) {
    var elapsedMs = 0L

    while (!predicate()) {
        SystemClock.sleep(busyWaitIntervalMs)
        elapsedMs += busyWaitIntervalMs
        if (elapsedMs >= timeoutMs) {
            throw java.lang.RuntimeException(errorMessage())
        }
    }
}
