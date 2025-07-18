/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.helpers;

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.test.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * PerfettoHelper is used to start and stop the perfetto tracing and move the
 * output perfetto trace file to destination folder.
 */
public class PerfettoHelper {

    private static final String LOG_TAG = PerfettoHelper.class.getSimpleName();
    // Command to start the perfetto tracing in the background. The "perfetto" process will wait
    // until tracing is fully started (i.e. all data sources are active) before backgrounding and
    // returning from the original shell invocation.
    //   perfetto --background-wait -c /data/misc/perfetto-traces/trace_config.pb -o
    //   /data/misc/perfetto-traces/trace_output.perfetto-trace
    private static final String PERFETTO_START_BG_WAIT_CMD =
            "perfetto --background-wait -c %s%s -o %s";
    private static final String PERFETTO_START_CMD = "perfetto --background -c %s%s -o %s";
    // Additional arg to indicate that the perfetto config file is text format.
    private static final String PERFETTO_TXT_PROTO_ARG = " --txt";
    // Command to stop (i.e kill) the perfetto tracing.
    private static final String PERFETTO_STOP_CMD = "kill %d";
    // Command to return the process details if it is still running otherwise returns empty string.
    private static final String PERFETTO_PROC_ID_EXIST_CHECK = "ls -l /proc/%d/exe";
    // Remove the trace output file /data/misc/perfetto-traces/trace_output.perfetto-trace
    private static final String REMOVE_CMD = "rm %s";
    // Add the trace output file /data/misc/perfetto-traces/trace_output.perfetto-trace
    private static final String CREATE_FILE_CMD = "touch %s";
    // Command to copy the perfetto output trace file to given folder.
    private static final String COPY_CMD = "cp %s %s";
    // Command to set the read permission for all users on the given file.
    private static final String CHMOD_READ_CMD = "chmod a+r %s";
    // Max wait count for checking if perfetto is stopped successfully
    private static final int PERFETTO_KILL_WAIT_COUNT = 12;
    // Check if perfetto is stopped every 5 secs.
    private static final long PERFETTO_KILL_WAIT_TIME = 5000;
    private static final String PERFETTO_PID_FILE_PREFIX = "perfetto_pid_";

    private static Set<Integer> sPerfettoProcessIds = new HashSet<>();

    private UiDevice mUIDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    private String mConfigRootDir;

    private boolean mPerfettoStartBgWait;

    private int mPerfettoProcId = 0;

    private String mTextProtoConfig;
    private String mConfigFileName;
    private boolean mIsTextProtoConfig;
    private boolean mTrackPerfettoPidFlag;
    private String mTrackPerfettoRootDir = "sdcard/";
    private File mPerfettoPidFile;

    private String mTmpOutputFilePath = "";

    public PerfettoHelper() {
        this("");
    }

    /**
     * Creates PerfettoHelper
     * @param tmpFilePrefix prefix for the temporary perfetto trace, must be unique for all
     *                      sessions happening at the same time
     */
    public PerfettoHelper(String tmpFilePrefix) {
        mTmpOutputFilePath = "/data/misc/perfetto-traces/" + tmpFilePrefix
                + "trace_output.perfetto-trace";
    }

    /** Set content of the perfetto configuration to be used when tracing */
    public PerfettoHelper setTextProtoConfig(String value) {
        mTextProtoConfig = value;
        return this;
    }

    /** Set file name of the perfetto configuration to be used when tracing */
    public PerfettoHelper setConfigFileName(String value) {
        mConfigFileName = value;
        return this;
    }

    /** Set if the configuration is in text proto format */
    public PerfettoHelper setIsTextProtoConfig(boolean value) {
        mIsTextProtoConfig = value;
        return this;
    }

    /**
     * Start the perfetto tracing in background using the given config file or config, and write the
     * output to /data/misc/perfetto-traces/trace_output.perfetto-trace. If both config file and
     * config are received, use config file
     *
     * @throws IllegalStateException if neither a config or a config file is set
     * @return true if trace collection started successfully otherwise return false.
     */
    public boolean startCollecting() {
        String textProtoConfig = mTextProtoConfig != null ? mTextProtoConfig : "";
        String configFileName = mConfigFileName != null ? mConfigFileName : "";
        if (textProtoConfig.isEmpty() && configFileName.isEmpty()) {
            throw new IllegalStateException(
                    "Perfetto helper not configured. Set a configuration "
                            + "or a configuration file before start tracing");
        }

        if (!textProtoConfig.isEmpty()) {
            return startCollectingFromConfig(mTextProtoConfig);
        }

        return startCollectingFromConfigFile(mConfigFileName, mIsTextProtoConfig);
    }

    /**
     * Start the perfetto tracing in background using the given config and write the output to
     * /data/misc/perfetto-traces/trace_output.perfetto-trace.
     *
     * @param textProtoConfig configuration in text proto format to pass to perfetto
     * @return true if trace collection started successfully otherwise return false.
     */
    @VisibleForTesting
    public boolean startCollectingFromConfig(String textProtoConfig) {
        mPerfettoPidFile = null;
        String startOutput = null;
        if (textProtoConfig == null || textProtoConfig.isEmpty()) {
            Log.e(LOG_TAG, "Perfetto config is null or empty.");
            return false;
        }

        try {
            if (!canSetupBeforeStartCollecting()) {
                return false;
            }

            String perfettoCmd =
                    String.format(
                            mPerfettoStartBgWait ? PERFETTO_START_BG_WAIT_CMD : PERFETTO_START_CMD,
                            "- ",
                            "--txt",
                            mTmpOutputFilePath);

            // Start perfetto tracing.
            Log.i(LOG_TAG, "Starting perfetto tracing.");
            UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            ParcelFileDescriptor[] fileDescriptor = uiAutomation.executeShellCommandRw(perfettoCmd);
            ParcelFileDescriptor inputStreamDescriptor = fileDescriptor[0];
            ParcelFileDescriptor outputStreamDescriptor = fileDescriptor[1];

            try (ParcelFileDescriptor.AutoCloseOutputStream outputStream =
                    new ParcelFileDescriptor.AutoCloseOutputStream(outputStreamDescriptor)) {
                outputStream.write(textProtoConfig.getBytes());
            }

            try (ParcelFileDescriptor.AutoCloseInputStream inputStream =
                    new ParcelFileDescriptor.AutoCloseInputStream(inputStreamDescriptor)) {
                startOutput = new String(inputStream.readAllBytes());
                // Persist perfetto pid in a file and use it for cleanup if the instrumentation
                // crashes.
                if (mTrackPerfettoPidFlag) {
                    mPerfettoPidFile = writePidToFile(startOutput);
                }
                if (!canUpdateAfterStartCollecting(startOutput)) {
                    return false;
                }
            }
        } catch (FileNotFoundException fnf) {
            Log.e(LOG_TAG, "Unable to write perfetto process id to a file :" + fnf.getMessage());
            Log.i(LOG_TAG, "Stopping perfetto tracing because perfetto id is not tracked.");
            try {
                stopPerfetto(Integer.parseInt(startOutput.trim()));
            } catch (IOException ie) {
                Log.e(LOG_TAG, "Unable to stop perfetto process output file." + ie.getMessage());
            }
            return false;
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to start the perfetto tracing due to :" + ioe.getMessage());
            return false;
        }
        Log.i(LOG_TAG, "Perfetto tracing started successfully.");
        return true;
    }

    /**
     * Start the perfetto tracing in background using the given config file and write the ouput to
     * /data/misc/perfetto-traces/trace_output.perfetto-trace. Perfetto has access only to
     * /data/misc/perfetto-traces/ folder. So the config file has to be under
     * /data/misc/perfetto-traces/ folder in the device.
     *
     * @param configFileName used for collecting the perfetto trace.
     * @param isTextProtoConfig true if the config file is textproto format otherwise false.
     * @return true if trace collection started successfully otherwise return false.
     */
    @VisibleForTesting
    public boolean startCollectingFromConfigFile(String configFileName, boolean isTextProtoConfig) {
        mPerfettoPidFile = null;
        String startOutput = null;
        if (configFileName == null || configFileName.isEmpty()) {
            Log.e(LOG_TAG, "Perfetto config file name is null or empty.");
            return false;
        }

        if (mConfigRootDir == null || mConfigRootDir.isEmpty()) {
            Log.e(LOG_TAG, "Perfetto trace config root directory name is null or empty.");
            return false;
        }

        try {
            if (!canSetupBeforeStartCollecting()) {
                return false;
            }

            String perfettoCmd =
                    String.format(
                            mPerfettoStartBgWait ? PERFETTO_START_BG_WAIT_CMD : PERFETTO_START_CMD,
                            mConfigRootDir,
                            configFileName,
                            mTmpOutputFilePath);

            if (isTextProtoConfig) {
                perfettoCmd = perfettoCmd + PERFETTO_TXT_PROTO_ARG;
            }

            // Start perfetto tracing.
            Log.i(LOG_TAG, "Starting perfetto tracing.");
            startOutput = mUIDevice.executeShellCommand(perfettoCmd);
            if (mTrackPerfettoPidFlag) {
                // Persist perfetto pid in a file and use it for cleanup if the instrumentation
                // crashes.
                mPerfettoPidFile = writePidToFile(startOutput);
            }
            Log.i(LOG_TAG, String.format("Perfetto start command output - %s", startOutput));

            if (!canUpdateAfterStartCollecting(startOutput)) {
                return false;
            }
        } catch (FileNotFoundException fnf) {
            Log.e(LOG_TAG, "Unable to write perfetto process id to a file :" + fnf.getMessage());
            Log.i(LOG_TAG, "Stopping perfetto tracing because perfetto id is not tracked.");
            try {
                stopPerfetto(Integer.parseInt(startOutput.trim()));
            } catch (IOException ie) {
                Log.e(LOG_TAG, "Unable to stop perfetto process output file." + ie.getMessage());
            }
            return false;
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to start the perfetto tracing due to :" + ioe.getMessage());
            return false;
        }
        Log.i(LOG_TAG, "Perfetto tracing started successfully.");
        return true;
    }

    private boolean canSetupBeforeStartCollecting() throws IOException {
        // Remove already existing temporary output trace file if any.
        String output =
                mUIDevice.executeShellCommand(String.format(REMOVE_CMD, mTmpOutputFilePath));
        Log.i(LOG_TAG, String.format("Perfetto output file cleanup - %s", output));

        // Create new temporary output trace file before tracing.
        output =
                mUIDevice.executeShellCommand(
                        String.format(CREATE_FILE_CMD, mTmpOutputFilePath));
        if (output.isEmpty()) {
            Log.i(LOG_TAG, "Perfetto output file create success.");
        } else {
            Log.e(LOG_TAG, String.format("Unable to create Perfetto output file - %s", output));
            return false;
        }

        return true;
    }

    private boolean canUpdateAfterStartCollecting(String startOutput) {
        Log.i(LOG_TAG, String.format("Perfetto start command output - %s", startOutput));

        if (!startOutput.isEmpty()) {
            mPerfettoProcId = Integer.parseInt(startOutput.trim());
            sPerfettoProcessIds.add(mPerfettoProcId);
            Log.i(
                    LOG_TAG,
                    String.format("Perfetto process id %d added for tracking", mPerfettoProcId));
        }

        // If the perfetto background wait option is not used then add a explicit wait after
        // starting the perfetto trace.
        if (!mPerfettoStartBgWait) {
            SystemClock.sleep(1000);
        }

        if (!isTestPerfettoRunning(mPerfettoProcId)) {
            return false;
        }

        return true;
    }

    /**
     * Stop the perfetto trace collection and redirect the output to
     * /data/misc/perfetto-traces/trace_output.perfetto-trace after waiting for given time in msecs
     * and copy the output to the destination file.
     * @param waitTimeInMsecs time to wait in msecs before stopping the trace collection.
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace collection is successfull otherwise false.
     */
    public boolean stopCollecting(long waitTimeInMsecs, String destinationFile) {
        // Wait for the dump interval before stopping the trace.
        Log.i(LOG_TAG, String.format(
                "Waiting for %d msecs before stopping perfetto.", waitTimeInMsecs));
        SystemClock.sleep(waitTimeInMsecs);

        // Stop the perfetto and copy the output file.
        Log.i(LOG_TAG, "Stopping perfetto.");
        try {
            if (stopPerfetto(mPerfettoProcId)) {
                if (!copyFileOutput(destinationFile)) {
                    return false;
                }
            } else {
                Log.e(LOG_TAG, "Perfetto failed to stop.");
                return false;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to stop the perfetto tracing due to " + ioe.getMessage());
            return false;
        }
        // Delete the perfetto process id file if the perfetto tracing successfully ended.
        if (mTrackPerfettoPidFlag) {
            if (mPerfettoPidFile.exists()) {
                Log.i(
                        LOG_TAG,
                        String.format(
                                "Deleting Perfetto process id file %s .",
                                mPerfettoPidFile.toString()));
                mPerfettoPidFile.delete();
            }
        }
        return true;
    }

    /**
     * Utility method for stopping perfetto.
     *
     * @param perfettoProcId perfetto process id.
     * @return true if perfetto is stopped successfully.
     */
    public boolean stopPerfetto(int perfettoProcId) throws IOException {
        Log.i(LOG_TAG, String.format("Killing the process id - %d", perfettoProcId));
        String stopOutput =
                mUIDevice.executeShellCommand(String.format(PERFETTO_STOP_CMD, perfettoProcId));
        Log.i(LOG_TAG, String.format("Perfetto stop command output - %s", stopOutput));
        int waitCount = 0;
        while (isTestPerfettoRunning(perfettoProcId)) {
            // 60 secs timeout for perfetto shutdown.
            if (waitCount < PERFETTO_KILL_WAIT_COUNT) {
                // Check every 5 secs if perfetto stopped successfully.
                SystemClock.sleep(PERFETTO_KILL_WAIT_TIME);
                waitCount++;
                continue;
            }
            Log.i(LOG_TAG, "Perfetto did not stop.");
            return false;
        }
        Log.i(LOG_TAG, "Perfetto stopped successfully.");
        boolean isRemoved = sPerfettoProcessIds.remove(perfettoProcId);
        Log.i(LOG_TAG, String.format("Process id removed status %s", Boolean.toString(isRemoved)));
        Log.i(
                LOG_TAG,
                String.format("Perfetto process id %d removed for tracking", perfettoProcId));
        return true;
    }

    /**
     * Utility method for writing perfetto pid to a file.
     *
     * @param perfettoStartOutput perfetto process id.
     * @return File with perfetto process id written in it.
     */
    private File writePidToFile(String perfettoStartOutput)
            throws IOException, FileNotFoundException {
        File perfettoPidFile =
                new File(
                        String.format(
                                "%s%s%s.txt",
                                getTrackPerfettoRootDir(),
                                PERFETTO_PID_FILE_PREFIX,
                                System.currentTimeMillis()));
        perfettoPidFile.createNewFile();
        try (PrintWriter out = new PrintWriter(perfettoPidFile)) {
            out.println(perfettoStartOutput);
            Log.i(
                    LOG_TAG,
                    String.format("Perfetto Process id file output %s", perfettoStartOutput));
        }
        Log.i(
                LOG_TAG,
                String.format("Perfetto Process id file %s created.", perfettoPidFile.toString()));
        return perfettoPidFile;
    }

    /**
     * Stop all the perfetto process from the given set.
     *
     * @param processIds set of perfetto process ids.
     * @return true if all the perfetto process is stopped otherwise false.
     */
    public boolean stopPerfettoProcesses(Set<Integer> processIds) throws IOException {
        boolean stopSuccess = true;
        for (int processId : processIds) {
            if (!stopPerfetto(processId)) {
                Log.i(
                        LOG_TAG,
                        String.format("Failed to stop the perfetto process id - %d", processId));
                stopSuccess = false;
            } else {
                Log.i(
                        LOG_TAG,
                        String.format(
                                "Successfully stopped the perfetto process id - %d", processId));
            }
        }
        return stopSuccess;
    }

    /**
     * Check if perfetto process is running or not.
     *
     * @param perfettoProcId perfetto process id.
     * @return true if perfetto is running otherwise false.
     */
    private boolean isTestPerfettoRunning(int perfettoProcId) {
        try {
            String perfettoProcStatus =
                    mUIDevice.executeShellCommand(
                            String.format(PERFETTO_PROC_ID_EXIST_CHECK, perfettoProcId));
            Log.i(LOG_TAG, String.format("Perfetto process id status check - %s",
                    perfettoProcStatus));
            // If proc details not empty then process is still running.
            if (!perfettoProcStatus.isEmpty()) {
                return true;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Not able to check the perfetto status due to:" + ioe.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Copy the temporary perfetto trace output file from /data/misc/perfetto-traces/ to given
     * destinationFile.
     *
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace file copied successfully otherwise false.
     */
    private boolean copyFileOutput(String destinationFile) {
        // Create the destination directory if it doesn't already exist.
        Path path = Paths.get(destinationFile);
        String destDirectory = path.getParent().toString();
        try {
            mUIDevice.executeShellCommand(String.format("mkdir -p %s", destDirectory));
        } catch (IOException ioe) {
            Log.e(
                    LOG_TAG,
                    String.format("Failed to create destination directory %s", destDirectory),
                    ioe);
            return false;
        }

        // Copy the collected trace from /data/misc/perfetto-traces/trace_output.perfetto-trace to
        // destinationFile
        // Note: Due to b/141386109, certain devices do not allow moving the files between
        //       directories with different encryption policies, so manually copy and then
        //       remove the original file
        //       Moreover, the copied trace file may end up with different permissions, resulting
        //       in b/162072200, to prevent this, ensure the files are readable after copying
        try {
            String copyResult = mUIDevice.executeShellCommand(String.format(
                    COPY_CMD, mTmpOutputFilePath, destinationFile));
            if (!copyResult.isEmpty()) {
                Log.e(LOG_TAG, String.format(
                        "Unable to copy perfetto output file from %s to %s due to %s",
                        mTmpOutputFilePath, destinationFile, copyResult));
                return false;
            }
            String chmodResult = mUIDevice.executeShellCommand(String.format(
                    CHMOD_READ_CMD, destinationFile));
            if (!chmodResult.isEmpty()) {
                Log.e(LOG_TAG, String.format(
                        "Unable to set read permission on perfetto output file %s due to %s",
                        destinationFile, chmodResult));
                return false;
            }
            // This will silently fail on unrooted devices due to missing sepolicy for file writing,
            // but it will be cleaned up by perfetto on the next run.
            String removeResult = mUIDevice.executeShellCommand(String.format(
                    REMOVE_CMD, mTmpOutputFilePath));
            if (!removeResult.isEmpty()) {
                Log.e(LOG_TAG, String.format(
                        "Unable to remove temporary perfetto output file %s due to %s",
                        mTmpOutputFilePath, removeResult));
                return false;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG,
                    "Unable to copy the perfetto trace file to destination file."
                            + ioe.getMessage());
            return false;
        }
        return true;
    }

    public void setPerfettoConfigRootDir(String rootDir) {
        mConfigRootDir = rootDir;
    }

    public void setPerfettoStartBgWait(boolean perfettoStartBgWait) {
        mPerfettoStartBgWait = perfettoStartBgWait;
    }

    public int getPerfettoPid() {
        return mPerfettoProcId;
    }

    public Set<Integer> getPerfettoPids() {
        return sPerfettoProcessIds;
    }

    public void setTrackPerfettoPidFlag(boolean trackPerfettoPidFlag) {
        mTrackPerfettoPidFlag = trackPerfettoPidFlag;
    }

    public boolean getTrackPerfettoPidFlag() {
        return mTrackPerfettoPidFlag;
    }

    public void setTrackPerfettoRootDir(String rootDir) {
        mTrackPerfettoRootDir = rootDir;
    }

    public String getTrackPerfettoRootDir() {
        return mTrackPerfettoRootDir;
    }

    public File getPerfettoPidFile() {
        return mPerfettoPidFile;
    }

    public String getPerfettoFilePrefix() {
        return PERFETTO_PID_FILE_PREFIX;
    }
}
