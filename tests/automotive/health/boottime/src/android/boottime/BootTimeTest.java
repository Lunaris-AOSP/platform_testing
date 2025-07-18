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

package android.boottime;

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.LogcatReceiver;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.FileInputStreamSource;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner.TestLogData;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner.TestMetrics;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.testtype.junit4.BeforeClassWithInfo;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.RunUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Performs successive reboots */
@RunWith(DeviceJUnit4ClassRunner.class)
@OptionClass(alias = "boot-time-test")
public class BootTimeTest extends BaseHostJUnit4Test {
    private static final String LOGCAT_CMD_ALL = "logcat -b all *:V";
    private static final String LOGCAT_CMD_CLEAR = "logcat -c";
    private static final long LOGCAT_SIZE = 80 * 1024 * 1024;
    private static final String DMESG_FILE = "/data/local/tmp/dmesglogs.txt";
    private static final String DUMP_DMESG = String.format("dmesg > %s", DMESG_FILE);
    private static final String LOGCAT_FILENAME = "Successive_reboots_logcat";
    private static final String DMESG_FILENAME = "Successive_reboots_dmesg";
    private static final String IMMEDIATE_DMESG_FILENAME = "Successive_reboots_immediate_dmesg";
    private static final String F2FS_SHUTDOWN_COMMAND = "f2fs_io shutdown 4 /data";
    private static final String F2FS_SHUTDOWN_SUCCESS_OUTPUT = "Shutdown /data with level=4";
    private static final String BOOT_TIME_PROP = "ro.boot.boottime";
    private static final String BOOT_TIME_PROP_KEY = "boot_time_prop";
    private static final String METRIC_KEY_SEPARATOR = "_";
    private static final String PERFETTO_TRACE_FILE_CHECK_CMD = "ls -l /data/misc/perfetto-traces";
    private static final String PERFETTO_TRACE_MV_CMD =
            "mv /data/misc/perfetto-traces/boottrace.perfetto-trace %s";
    private static final String PERFETTO_FILE_PATH = "perfetto_file_path";
    private static final int BOOT_COMPLETE_POLL_INTERVAL = 1000;
    private static final int BOOT_COMPLETE_POLL_RETRY_COUNT = 45;
    private static final String PACKAGE_NAME = "com.android.boothelper";
    private static final String CLASS_NAME = "com.android.boothelper.BootHelperTest";
    private static final String RUNNER = "androidx.test.runner.AndroidJUnitRunner";
    private static final String UNLOCK_PIN_TEST = "unlockScreenWithPin";
    private static final String SETUP_PIN_TEST = "setupLockScreenPin";

    public static String getBootTimePropKey() {
        return BOOT_TIME_PROP_KEY;
    }

    @Option(
            name = "boot-count",
            description =
                    "Number of times to boot the devices to calculate the successive boot delay."
                            + " Second boot after the first boot will be skipped for correctness.")
    private int mBootCount = 5;

    @Option(
            name = "boot-delay",
            isTimeVal = true,
            description = "Time to wait between the successive boots.")
    private long mBootDelayTime = 2000;

    @Option(
            name = "after-boot-delay",
            isTimeVal = true,
            description = "Time to wait immediately after the successive boots.")
    private long mAfterBootDelayTime = 0;

    @Option(name = "device-boot-time", description = "Max time in ms to wait for device to boot.")
    protected long mDeviceBootTime = 5 * 60 * 1000;

    @Option(
            name = "successive-boot-prepare-cmd",
            description =
                    "A list of adb commands to run after first boot to prepare for successive"
                            + " boot tests")
    private List<String> mDeviceSetupCommands = new ArrayList<>();

    /**
     * Use this flag not to dump the dmesg logs immediately after the device is online. Use it only
     * if some of the boot dmesg logs are cleared when waiting until boot completed. By default this
     * is set to true which might result in duplicate logging.
     */
    @Option(
            name = "dump-dmesg-immediate",
            description =
                    "Whether to dump the dmesg logs" + "immediately after the device is online")
    private boolean mDumpDmesgImmediate = true;

    @Option(
            name = "force-f2fs-shutdown",
            description = "Force f2fs shutdown to trigger fsck check during the reboot.")
    private boolean mForceF2FsShutdown = false;

    @Option(
            name = "skip-pin-setup",
            description =
                    "Skip the pin setup if already set once"
                            + "and not needed for the second run especially in local testing.")
    private boolean mSkipPinSetup = false;

    @Option(
            name = "enable-perfetto-trace",
            description = "Enable perfetto trace collection during the reboot. True by default.")
    private boolean mEnablePerfettoTrace = true;

    private LogcatReceiver mRebootLogcatReceiver = null;
    private IRemoteAndroidTestRunner mPostBootTestRunner = null;

    @Rule public TestLogData testLog = new TestLogData();
    @Rule public TestMetrics testMetrics = new TestMetrics();

    /**
     * Prepares the device for successive boots
     *
     * @param testInfo Test Information
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *     recovered.
     */
    @BeforeClassWithInfo
    public static void beforeClassWithDevice(TestInformation testInfo)
            throws DeviceNotAvailableException {
        ITestDevice testDevice = testInfo.getDevice();

        testDevice.enableAdbRoot();
        testDevice.setDate(null);
        testDevice.nonBlockingReboot();
        testDevice.waitForDeviceOnline();
        testDevice.waitForDeviceAvailable(0);
    }

    @Before
    public void setUp() throws Exception {
        mPostBootTestRunner = null;
        setUpDeviceForSuccessiveBoots();
        CLog.v("Waiting for %d msecs before successive boots.", mBootDelayTime);
        sleep(mBootDelayTime);
        mRebootLogcatReceiver = new LogcatReceiver(getDevice(), LOGCAT_CMD_ALL, LOGCAT_SIZE, 0);
        mRebootLogcatReceiver.start();
    }

    @Test
    public void testSuccessiveBoots() throws Exception {
        for (int count = 0; count < mBootCount; count++) {
            testSuccessiveBoot(count);
        }
    }

    @Test
    public void testSuccessiveBootsDismissPin() throws Exception {
        // If pin is already set skip the setup method otherwise setup the pin.
        if (!mSkipPinSetup) {
            getDevice()
                    .runInstrumentationTests(
                            createRemoteAndroidTestRunner(SETUP_PIN_TEST),
                            new CollectingTestListener());
        }
        mPostBootTestRunner = createRemoteAndroidTestRunner(UNLOCK_PIN_TEST);
        for (int count = 0; count < mBootCount; count++) {
            testSuccessiveBoot(count);
        }
    }

    public void testSuccessiveBoot(int iteration) throws Exception {
        CLog.v("Successive boot iteration %d", iteration);
        getDevice().enableAdbRoot();
        // Property used for collecting the perfetto trace file on boot.
        String setPerfettoPropValue = mEnablePerfettoTrace ? "1" : "\"\"";
        String perfettoPropCmd = String.format("setprop persist.debug.perfetto.boottrace %s", setPerfettoPropValue);
        getDevice().executeShellCommand(perfettoPropCmd);
        if (mForceF2FsShutdown) {
            forseF2FsShutdown();
        }
        clearLogcat();
        sleep(5000);
        getDevice().nonBlockingReboot();
        getDevice().waitForDeviceOnline(mDeviceBootTime);
        getDevice().enableAdbRoot();
        if (mDumpDmesgImmediate) {
            saveDmesgInfo(
                    String.format(
                            "%s%s%d", IMMEDIATE_DMESG_FILENAME, METRIC_KEY_SEPARATOR, iteration));
        }
        waitForBootComplete();
        CLog.v("Waiting for %d msecs immediately after successive boot.", mAfterBootDelayTime);
        sleep(mAfterBootDelayTime);
        if (mPostBootTestRunner != null) {
            sleep(2000);
            getDevice().runInstrumentationTests(mPostBootTestRunner, new CollectingTestListener());
        }
        saveDmesgInfo(String.format("%s%s%d", DMESG_FILENAME, METRIC_KEY_SEPARATOR, iteration));
        saveLogcatInfo(iteration);
        // TODO(b/288323866): figure out why is the prop value null
        String bootLoaderVal = getDevice().getProperty(BOOT_TIME_PROP);
        // Sample Output : 1BLL:89,1BLE:590,2BLL:0,2BLE:1344,SW:6734,KL:1193
        CLog.d("%s value is %s", BOOT_TIME_PROP, bootLoaderVal);
        testMetrics.addTestMetric(
                String.format("%s%s%d", BOOT_TIME_PROP_KEY, METRIC_KEY_SEPARATOR, iteration),
                bootLoaderVal == null ? "" : bootLoaderVal);
        String perfettoTraceFilePath =
                processPerfettoFile(
                        String.format(
                                "%s%s%d",
                                BootTimeTest.class.getSimpleName(),
                                METRIC_KEY_SEPARATOR,
                                iteration));
        if (perfettoTraceFilePath != null) {
            testMetrics.addTestMetric(
                    String.format("%s%s%d", PERFETTO_FILE_PATH, METRIC_KEY_SEPARATOR, iteration),
                    perfettoTraceFilePath);
        }
    }

    private void setUpDeviceForSuccessiveBoots() throws DeviceNotAvailableException {
        for (String cmd : mDeviceSetupCommands) {
            CommandResult result;
            result = getDevice().executeShellV2Command(cmd);
            if (!CommandStatus.SUCCESS.equals(result.getStatus())) {
                CLog.w(
                        "Post boot setup cmd: '%s' failed, returned:\nstdout:%s\nstderr:%s",
                        cmd, result.getStdout(), result.getStderr());
            }
        }
    }

    private void forseF2FsShutdown() throws DeviceNotAvailableException {
        String output = getDevice().executeShellCommand(F2FS_SHUTDOWN_COMMAND).trim();
        if (!F2FS_SHUTDOWN_SUCCESS_OUTPUT.equalsIgnoreCase(output)) {
            CLog.e("Unable to shutdown the F2FS.");
        } else {
            CLog.i("F2FS shutdown successful.");
        }
    }

    private void saveLogcatInfo(int iteration) {
        try (InputStreamSource logcat = mRebootLogcatReceiver.getLogcatData()) {
            String testLogKey =
                    String.format("%s%s%d", LOGCAT_FILENAME, METRIC_KEY_SEPARATOR, iteration);
            testLog.addTestLog(testLogKey, LogDataType.LOGCAT, logcat);
        }
    }

    private void saveDmesgInfo(String filename) throws DeviceNotAvailableException {
        getDevice().executeShellCommand(DUMP_DMESG);
        File dmesgFile = getDevice().pullFile(DMESG_FILE);
        testLog.addTestLog(
                filename, LogDataType.HOST_LOG, new FileInputStreamSource(dmesgFile, false));
    }

    private void clearLogcat() throws DeviceNotAvailableException {
        getDevice().executeShellCommand(LOGCAT_CMD_CLEAR);
        mRebootLogcatReceiver.clear();
    }

    private void sleep(long duration) {
        RunUtil.getDefault().sleep(duration);
    }

    /**
     * Look for the perfetto trace file collected during reboot under /data/misc/perfetto-traces and
     * copy the file under /data/local/tmp using the test iteration name and return the path to the
     * newly copied trace file.
     */
    private String processPerfettoFile(String testId) throws DeviceNotAvailableException {
        CommandResult result = getDevice().executeShellV2Command(PERFETTO_TRACE_FILE_CHECK_CMD);
        if (result != null) {
            CLog.i(
                    "Command Output: Cmd - %s, Output - %s, Error - %s, Status - %s",
                    PERFETTO_TRACE_FILE_CHECK_CMD,
                    result.getStdout(),
                    result.getStderr(),
                    result.getStatus());
            if (CommandStatus.SUCCESS.equals(result.getStatus())
                    && result.getStdout().contains("boottrace.perfetto-trace")) {
                // Move the perfetto trace file to the new location and rename it using the test
                // name.
                String finalTraceFileLocation =
                        String.format("/data/local/tmp/%s.perfetto-trace", testId);
                CommandResult moveResult =
                        getDevice()
                                .executeShellV2Command(
                                        String.format(
                                                PERFETTO_TRACE_MV_CMD, finalTraceFileLocation));
                if (moveResult != null) {
                    CLog.i(
                            "Command Output: Cmd - %s, Output - %s, Error - %s, Status - %s",
                            PERFETTO_TRACE_MV_CMD,
                            moveResult.getStdout(),
                            moveResult.getStderr(),
                            moveResult.getStatus());
                    if (CommandStatus.SUCCESS.equals(result.getStatus())) {
                        return finalTraceFileLocation;
                    }
                }
            }
        }
        return null;
    }

    private void waitForBootComplete() throws DeviceNotAvailableException {
        for (int i = 0; i < BOOT_COMPLETE_POLL_RETRY_COUNT; i++) {
            if (isBootCompleted()) {
                return;
            }
            CLog.v(String.format("waitForBootComplete %d", i));
            sleep(BOOT_COMPLETE_POLL_INTERVAL);
        }
        throw new DeviceNotAvailableException(
                "Can't confirm boot complete. Exhausted retries. sys.boot_completed property does"
                        + " not equal 1.",
                getDevice().getSerialNumber());
    }

    private boolean isBootCompleted() throws DeviceNotAvailableException {
        return "1".equals(getDevice().getProperty("sys.boot_completed"));
    }

    /**
     * Method to create the runner with given testName
     *
     * @return the {@link IRemoteAndroidTestRunner} to use.
     */
    IRemoteAndroidTestRunner createRemoteAndroidTestRunner(String testName)
            throws DeviceNotAvailableException {
        RemoteAndroidTestRunner runner =
                new RemoteAndroidTestRunner(PACKAGE_NAME, RUNNER, getDevice().getIDevice());
        runner.setMethodName(CLASS_NAME, testName);
        return runner;
    }
}
