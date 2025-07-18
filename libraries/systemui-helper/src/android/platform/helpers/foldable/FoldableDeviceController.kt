/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.platform.helpers.foldable

import android.hardware.Sensor
import android.hardware.devicestate.DeviceState.PROPERTY_FEATURE_REAR_DISPLAY
import android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_INNER_PRIMARY
import android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY
import android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_HALF_OPEN
import android.hardware.devicestate.DeviceStateManager
import android.hardware.devicestate.DeviceStateManager.DeviceStateCallback
import android.hardware.devicestate.DeviceStateManager.INVALID_DEVICE_STATE_IDENTIFIER
import android.hardware.devicestate.DeviceStateRequest
import android.hardware.devicestate.feature.flags.Flags as DeviceStateManagerFlags
import android.platform.test.rule.isLargeScreen
import android.platform.uiautomatorhelpers.DeviceHelpers.isScreenOnSettled
import android.platform.uiautomatorhelpers.DeviceHelpers.printInstrumentationStatus
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.TracingUtils.trace
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.util.Log
import androidx.annotation.FloatRange
import androidx.test.platform.app.InstrumentationRegistry
import com.android.internal.R
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates.notNull
import org.junit.Assume.assumeTrue

/** Helper to set the folded state to a device. */
internal class FoldableDeviceController {

    private val context = InstrumentationRegistry.getInstrumentation().context

    private val resources = context.resources
    private val deviceStateManager = context.getSystemService(DeviceStateManager::class.java)!!
    private val hingeAngleSensor = SensorInjectionController(Sensor.TYPE_HINGE_ANGLE)

    private var foldedState by notNull<Int>()
    private var unfoldedState by notNull<Int>()
    private var halfFoldedState by notNull<Int>()
    private var rearDisplayState by notNull<Int>()
    private var currentState: Int? = null

    private var deviceStateLatch = CountDownLatch(1)
    private var pendingRequest: DeviceStateRequest? = null

    /** Sets device state to folded. */
    fun fold() {
        trace("FoldableDeviceController#fold") {
            printInstrumentationStatus(TAG, "Folding")
            setHingeAngle(0f)
            setDeviceState(foldedState)
        }
    }

    /** Sets device state to an unfolded state. */
    fun unfold() {
        trace("FoldableDeviceController#unfold") {
            printInstrumentationStatus(TAG, "Unfolding")
            setHingeAngle(180f)
            setDeviceState(unfoldedState)
        }
    }

    /** Sets device state to half folded. */
    fun halfFold() {
        trace("FoldableDeviceController#halfFold") {
            printInstrumentationStatus(TAG, "Half-folding")
            setHingeAngle(100f)
            setDeviceState(halfFoldedState)
        }
    }

    /** Sets device state to rear display */
    fun rearDisplay() {
        trace("FoldableDeviceController#rearDisplay") {
            printInstrumentationStatus(TAG, "Rear display")
            setDeviceState(rearDisplayState)
        }
    }

    /** Removes the override on the device state. */
    private fun resetDeviceState() {
        printInstrumentationStatus(TAG, "resetDeviceState")
        deviceStateManager.cancelBaseStateOverride()
        // This might cause the screen to turn off if the default state is folded.
        if (!uiDevice.isScreenOnSettled) {
            uiDevice.wakeUp()
            ensureThat("screen is on after cancelling base state override.") { uiDevice.isScreenOn }
        }
    }

    fun init() {
        deviceStateManager.registerCallback(context.mainExecutor, deviceStateCallback)
        findStates()
        hingeAngleSensor.init()
    }

    fun uninit() {
        deviceStateManager.unregisterCallback(deviceStateCallback)
        resetDeviceState()
        hingeAngleSensor.uninit()
    }

    val isFolded: Boolean
        get() = currentState == foldedState

    val isUnfolded: Boolean
        get() = currentState == unfoldedState

    val isHalfFolded: Boolean
        get() = currentState == halfFoldedState

    val isOnRearDisplay: Boolean
        get() = currentState == rearDisplayState

    fun setHingeAngle(@FloatRange(from = 0.0, to = 180.0) angle: Float) {
        hingeAngleSensor.setValue(angle)
    }

    private fun findStates() {
        if (DeviceStateManagerFlags.deviceStatePropertyMigration()) {
            findStates_deviceStateManager()
        } else {
            findStates_configValues()
        }
    }

    private fun findStates_configValues() {
        val foldedStates = resources.getIntArray(R.array.config_foldedDeviceStates)
        assumeTrue("Skipping on non-foldable devices", foldedStates.isNotEmpty())
        foldedState = foldedStates.first()
        unfoldedState = resources.getIntArray(R.array.config_openDeviceStates).first()
        halfFoldedState = resources.getIntArray(R.array.config_halfFoldedDeviceStates).first()
        rearDisplayState = resources.getIntArray(R.array.config_rearDisplayDeviceStates).first()
    }

    private fun findStates_deviceStateManager() {
        val deviceStates = deviceStateManager.supportedDeviceStates
        foldedState =
            deviceStates
                .firstOrNull { deviceState ->
                    deviceState.hasProperty(
                        PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY
                    ) && !deviceState.hasProperty(PROPERTY_FEATURE_REAR_DISPLAY)
                }
                ?.identifier ?: INVALID_DEVICE_STATE_IDENTIFIER

        assumeTrue(
            "Skipping on non-foldable devices",
            foldedState != INVALID_DEVICE_STATE_IDENTIFIER,
        )

        halfFoldedState =
            deviceStates
                .firstOrNull { deviceState ->
                    deviceState.hasProperties(
                        PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_INNER_PRIMARY,
                        PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_HALF_OPEN,
                    )
                }
                ?.identifier ?: INVALID_DEVICE_STATE_IDENTIFIER

        unfoldedState =
            deviceStates
                .firstOrNull { deviceState ->
                    deviceState.hasProperty(
                        PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_INNER_PRIMARY
                    ) &&
                        !deviceState.hasProperty(
                            PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_HALF_OPEN
                        )
                }
                ?.identifier ?: INVALID_DEVICE_STATE_IDENTIFIER

        rearDisplayState =
            deviceStates
                .firstOrNull { deviceState ->
                    deviceState.hasProperty(PROPERTY_FEATURE_REAR_DISPLAY)
                }
                ?.identifier ?: INVALID_DEVICE_STATE_IDENTIFIER
    }

    private fun setDeviceState(state: Int) {
        if (currentState == state) {
            Log.e(TAG, "setting device state to the same state already set.")
            return
        }
        deviceStateLatch = CountDownLatch(1)
        val request = DeviceStateRequest.newBuilder(state).build()
        pendingRequest = request
        trace("Requesting base state override to ${state.desc()}") {
            deviceStateManager.requestBaseStateOverride(
                request,
                context.mainExecutor,
                deviceStateRequestCallback,
            )
            deviceStateLatch.await { "Device state didn't change within the timeout" }
            ensureStateSet(state)
        }
        Log.d(TAG, "Device state set to ${state.desc()}")
    }

    private fun ensureStateSet(state: Int) {
        when (state) {
            foldedState ->
                ensureThat("Device folded") { currentState == foldedState && !isLargeScreen() }
            unfoldedState ->
                ensureThat("Device unfolded") { currentState == unfoldedState && isLargeScreen() }
            halfFoldedState ->
                ensureThat("Device half folded") {
                    currentState == halfFoldedState && isLargeScreen()
                }
            rearDisplayState ->
                ensureThat("Device rear display") {
                    currentState == rearDisplayState && !isLargeScreen()
                }
        }
    }

    private fun Int.desc() =
        when (this) {
            foldedState -> "Folded"
            unfoldedState -> "Unfolded"
            halfFoldedState -> "Half Folded"
            rearDisplayState -> "Rear Display"
            else -> "unknown"
        }

    private fun CountDownLatch.await(error: () -> String) {
        check(this.await(DEVICE_STATE_MAX_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS), error)
    }

    private val deviceStateCallback = DeviceStateCallback { state ->
        currentState = state.identifier
    }

    private val deviceStateRequestCallback =
        object : DeviceStateRequest.Callback {
            override fun onRequestActivated(request: DeviceStateRequest) {
                Log.d(TAG, "Request activated: ${request.state.desc()}")
                if (request == pendingRequest) {
                    deviceStateLatch.countDown()
                }
                currentState = request.state
            }

            override fun onRequestCanceled(request: DeviceStateRequest) {
                Log.d(TAG, "Request cancelled: ${request.state.desc()}")
                if (currentState == request.state) {
                    currentState = null
                }
            }

            override fun onRequestSuspended(request: DeviceStateRequest) {
                Log.d(TAG, "Request suspended: ${request.state.desc()}")
                if (currentState == request.state) {
                    currentState = null
                }
            }
        }

    private companion object {
        const val TAG = "FoldableController"
        val DEVICE_STATE_MAX_TIMEOUT = Duration.ofSeconds(10)
    }
}
