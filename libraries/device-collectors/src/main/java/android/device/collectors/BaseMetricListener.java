/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.device.collectors;

import android.device.collectors.annotations.MetricOption;
import android.device.collectors.annotations.OptionClass;
import android.device.collectors.util.SendToInstrumentation;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.Trace;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.listener.InstrumentationRunListener;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Base implementation of a device metric listener that will capture and output metrics for each
 * test run or test cases. Collectors will have access to {@link DataRecord} objects where they
 * can put results and the base class ensure these results will be send to the instrumentation.
 *
 * Unless the shell is used to write files, any subclass that uses the directory created with
 * {@link #createAndEmptyDirectory(String)} needs external storage permission. So to use this class
 * at runtime in such subclasses, your test need to
 * <a href="{@docRoot}training/basics/data-storage/files.html#GetWritePermission">have storage
 * permission enabled</a>, and preferably granted at install time (to avoid interrupting the test).
 * For testing at desk, run adb install -r -g testpackage.apk
 * "-g" grants all required permission at install time.
 *
 * Filtering:
 * You can annotate any test method (@Test) with {@link MetricOption} and specify an arbitrary
 * group name that the test will be part of. It is possible to trigger the collection only against
 * test part of a group using '--include-filter-group [group name]' or to exclude a particular
 * group using '--exclude-filter-group [group name]'.
 * Several group name can be passed using a comma separated argument.
 *
 */
public class BaseMetricListener extends InstrumentationRunListener {

    public static final int BUFFER_SIZE = 1024;
    // Default collect iteration interval.
    private static final int DEFAULT_COLLECT_INTERVAL = 1;

    // Default skip metric until iteration count.
    private static final int SKIP_UNTIL_DEFAULT_ITERATION = 0;

    /** Options keys that the collector can receive. */
    // Filter groups, comma separated list of group name to be included or excluded
    public static final String INCLUDE_FILTER_GROUP_KEY = "include-filter-group";
    public static final String EXCLUDE_FILTER_GROUP_KEY = "exclude-filter-group";
    // Argument passed to AndroidJUnitRunner to make it log-only, we shouldn't collect on log only.
    public static final String ARGUMENT_LOG_ONLY = "log";
    // Collect metric every nth iteration of a test with the same name.
    public static final String COLLECT_ITERATION_INTERVAL = "collect_iteration_interval";

    // Skip metric collection until given n iteration. Uses 1 indexing here.
    // For example if overall iteration is 10 and skip until iteration is set
    // to 3. Metric will not be collected for 1st,2nd and 3rd iteration.
    public static final String SKIP_METRIC_UNTIL_ITERATION = "skip_metric_until_iteration";

    private static final String NAMESPACE_SEPARATOR = ":";

    private DataRecord mRunData;
    private DataRecord mTestData;

    private Bundle mArgsBundle = null;
    private final List<String> mIncludeFilters;
    private final List<String> mExcludeFilters;
    private boolean mLogOnly = false;
    // Store the method name and invocation count.
    private Map<String, Integer> mTestIdInvocationCount = new HashMap<>();
    private int mCollectIterationInterval = 1;
    private int mSkipMetricUntilIteration = 0;

    // Whether to report the results as instrumentation results. Used by metric collector rules,
    // which do not have the information to invoke InstrumentationRunFinished() to report metrics.
    private boolean mReportAsInstrumentationResults = false;

    public BaseMetricListener() {
        mIncludeFilters = new ArrayList<>();
        mExcludeFilters = new ArrayList<>();
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    protected BaseMetricListener(Bundle argsBundle) {
        this();
        mArgsBundle = argsBundle;
    }

    @Override
    public final void testRunStarted(Description description) throws Exception {
        Trace.beginSection(this.getClass().getSimpleName() + ":testRunStarted");
        setUp();
        if (!mLogOnly) {
            try {
                mRunData = createDataRecord();
                onTestRunStart(mRunData, description);
            } catch (RuntimeException e) {
                // Prevent exception from reporting events.
                Log.e(getTag(), "Exception during onTestRunStart.", e);
            }
        }
        super.testRunStarted(description);
        Trace.endSection();
    }

    @Override
    public final void testRunFinished(Result result) throws Exception {
        Trace.beginSection(this.getClass().getSimpleName() + ":testRunFinished");
        if (!mLogOnly) {
            try {
                onTestRunEnd(mRunData, result);
            } catch (RuntimeException e) {
                // Prevent exception from reporting events.
                Log.e(getTag(), "Exception during onTestRunEnd.", e);
            }
        }
        cleanUp();
        super.testRunFinished(result);
        Trace.endSection();
    }

    @Override
    public final void testStarted(Description description) throws Exception {
        Trace.beginSection(this.getClass().getSimpleName() + ":testStarted");
        // Update the current invocation before proceeding with metric collection.
        // mTestIdInvocationCount uses 1 indexing.
        mTestIdInvocationCount.compute(description.toString(),
                (key, value) -> (value == null) ? 1 : value + 1);

        if (shouldRun(description)) {
            try {
                mTestData = createDataRecord();
                onTestStart(mTestData, description);
            } catch (RuntimeException e) {
                // Prevent exception from reporting events.
                Log.e(getTag(), "Exception during onTestStart.", e);
            }
        }
        super.testStarted(description);
        Trace.endSection();
    }

    @Override
    public final void testFailure(Failure failure) throws Exception {
        Description description = failure.getDescription();
        if (shouldRun(description)) {
            try {
                onTestFail(mTestData, description, failure);
            } catch (RuntimeException e) {
                // Prevent exception from reporting events.
                Log.e(getTag(), "Exception during onTestFail.", e);
            }
        }
        super.testFailure(failure);
    }

    @Override
    public final void testFinished(Description description) throws Exception {
        Trace.beginSection(this.getClass().getSimpleName() + ":testFinished");
        if (shouldRun(description)) {
            try {
                onTestEnd(mTestData, description);
            } catch (RuntimeException e) {
                // Prevent exception from reporting events.
                Log.e(getTag(), "Exception during onTestEnd.", e);
            }
            if (mTestData.hasMetrics()) {
                // Only send the status progress if there are metrics
                if (mReportAsInstrumentationResults) {
                    getInstrumentation().addResults(mTestData.createBundleFromMetrics());
                } else {
                SendToInstrumentation.sendBundle(getInstrumentation(),
                        mTestData.createBundleFromMetrics());
            }
            }
        }
        super.testFinished(description);
        Trace.endSection();
    }

    @Override
    public void instrumentationRunFinished(
            PrintStream streamResult, Bundle resultBundle, Result junitResults) {
        // Test Run data goes into the INSTRUMENTATION_RESULT
        if (mRunData != null) {
            resultBundle.putAll(mRunData.createBundleFromMetrics());
        }
    }

    /**
     * Set up the metric collector.
     *
     * <p>If another class is invoking the metric collector's callbacks directly, it should call
     * this method to make sure that the metric collector is set up properly.
     */
    public final void setUp() {
        parseArguments();
        setupAdditionalArgs();
        onSetUp();
    }

    /**
     * Clean up the metric collector.
     *
     * <p>If another class is invoking the metric collector's callbacks directly, it should call
     * this method to make sure that the metric collector is cleaned up properly after collection.
     */
    public final void cleanUp() {
        onCleanUp();
    }

    /**
     * Create a {@link DataRecord}. Exposed for testing.
     */
    @VisibleForTesting
    DataRecord createDataRecord() {
        return new DataRecord();
    }

    // ---------- Interfaces that can be implemented to set up and clean up metric collection.

    /** Called if custom set-up is needed for this metric collector. */
    protected void onSetUp() {
        // Does nothing by default.
    }

    protected void onCleanUp() {
        // Does nothing by default.
    }

    // ---------- Interfaces that can be implemented to take action on each test state.

    /**
     * Called when {@link #testRunStarted(Description)} is called.
     *
     * @param runData structure where metrics can be put.
     * @param description the {@link Description} for the run about to start.
     */
    public void onTestRunStart(DataRecord runData, Description description) {
        // Does nothing
    }

    /**
     * Called when {@link #testRunFinished(Result result)} is called.
     *
     * @param runData structure where metrics can be put.
     * @param result the {@link Result} for the run coming from the runner.
     */
    public void onTestRunEnd(DataRecord runData, Result result) {
        // Does nothing
    }

    /**
     * Called when {@link #testStarted(Description)} is called.
     *
     * @param testData structure where metrics can be put.
     * @param description the {@link Description} for the test case about to start.
     */
    public void onTestStart(DataRecord testData, Description description) {
        // Does nothing
    }

    /**
     * Called when {@link #testFailure(Failure)} is called.
     *
     * @param testData structure where metrics can be put.
     * @param description the {@link Description} for the test case that just failed.
     * @param failure the {@link Failure} describing the failure.
     */
    public void onTestFail(DataRecord testData, Description description, Failure failure) {
        // Does nothing
    }

    /**
     * Called when {@link #testFinished(Description)} is called.
     *
     * @param testData structure where metrics can be put.
     * @param description the {@link Description} of the test coming from the runner.
     */
    public void onTestEnd(DataRecord testData, Description description) {
        // Does nothing
    }

    /**
     * To add listener-specific extra args, implement this method in the sub class and add the
     * listener specific args.
     */
    public void setupAdditionalArgs() {
        // NO-OP by default
    }

    /**
     * Turn executeShellCommand into a blocking operation.
     *
     * @param command shell command to be executed.
     * @return byte array of execution result
     */
    public byte[] executeCommandBlocking(String command) {
        try (
                InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(
                        getInstrumentation().getUiAutomation().executeShellCommand(command));
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            byte[] buf = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buf)) >= 0) {
                out.write(buf, 0, length);
            }
            return out.toByteArray();
        } catch (IOException e) {
            Log.e(getTag(), "Error executing: " + command, e);
            return null;
        }
    }

    private boolean fileExists(File file) {
        final byte[] cmdOut = executeCommandBlocking("ls -d " + file.getAbsolutePath());
        return cmdOut != null && cmdOut.length > 0;
    }

    /**
     * Create a directory inside external storage, and optionally empty it.
     *
     * @param dir full path to the dir to be created.
     * @param empty whether to empty the new dirctory.
     * @return directory file created
     */
    public File createDirectory(String dir, boolean empty) {
        File rootDir = Environment.getExternalStorageDirectory();
        File destDir = new File(rootDir, dir);
        if (empty) {
            executeCommandBlocking("rm -rf " + destDir.getAbsolutePath());
        }
        if (!fileExists(destDir)) {
            executeCommandBlocking("mkdir -p " + destDir.getAbsolutePath());
        }
        if (!fileExists(destDir)) {
            Log.e(getTag(), "Unable to create dir: " + destDir.getAbsolutePath());
            return null;
        }
        return destDir;
    }

    /**
     * Create a directory inside external storage, and empty it.
     *
     * @param dir full path to the dir to be created.
     * @return directory file created
     */
    public File createAndEmptyDirectory(String dir) {
        return createDirectory(dir, true);
    }

    /**
     * Get an OutputStream to a file using the shell.
     *
     * This allows tests to write to files without requiring storage permissions, which is in
     * particular useful when testing apps that should not have the permission.
     * @param file The file where the OutputStream should write to. Will be deleted if existing.
     * @return A stream to write to the file.
     */
    public OutputStream getOutputStreamViaShell(File file) throws IOException {
        if (fileExists(file)) {
            Log.w(getTag(), String.format("File exists: %s", file.getAbsolutePath()));
            recursiveDelete(file);
        }

        final ParcelFileDescriptor[] fds = getInstrumentation().getUiAutomation()
            .executeShellCommandRw("sh");

        fds[0].close();
        final ParcelFileDescriptor stdin = fds[1];
        final OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(stdin);

        final String cmd = "cat > " + file.getAbsolutePath() + "\n";
        os.write(cmd.getBytes(StandardCharsets.UTF_8));

        return os;
    }

    /**
     * Delete a directory and all the files inside.
     *
     * @param rootDir the {@link File} directory or file to delete.
     */
    public void recursiveDelete(File rootDir) {
        if (rootDir != null) {
            executeCommandBlocking("rm -r " + rootDir.getAbsolutePath());
        }
    }

    /** Sets whether metrics should be reported directly to instrumentation results. */
    public final void setReportAsInstrumentationResults(boolean enabled) {
        mReportAsInstrumentationResults = enabled;
    }

    /**
     * Returns the name of the current class to be used as a logging tag.
     */
    String getTag() {
        return this.getClass().getName();
    }

    /**
     * Returns the bundle containing the instrumentation arguments.
     */
    protected final Bundle getArgsBundle() {
        if (mArgsBundle == null) {
            mArgsBundle = InstrumentationRegistry.getArguments();
        }
        return mArgsBundle;
    }

    protected void parseArguments() {
        Bundle args = getArgsBundle();
        // First filter the arguments with the alias
        filterAlias(args);
        // Handle filtering
        String includeGroup = args.getString(INCLUDE_FILTER_GROUP_KEY);
        String excludeGroup = args.getString(EXCLUDE_FILTER_GROUP_KEY);
        if (includeGroup != null) {
            mIncludeFilters.addAll(Arrays.asList(includeGroup.split(",")));
        }
        if (excludeGroup != null) {
            mExcludeFilters.addAll(Arrays.asList(excludeGroup.split(",")));
        }
        mCollectIterationInterval = Integer.parseInt(args.getString(
                COLLECT_ITERATION_INTERVAL, String.valueOf(DEFAULT_COLLECT_INTERVAL)));
        mSkipMetricUntilIteration = Integer.parseInt(args.getString(
                SKIP_METRIC_UNTIL_ITERATION, String.valueOf(SKIP_UNTIL_DEFAULT_ITERATION)));

        if (mCollectIterationInterval < 1) {
            Log.i(getTag(), "Metric collection iteration interval cannot be less than 1."
                    + "Switching to collect for all the iterations.");
            // Reset to collect for all the iterations.
            mCollectIterationInterval = 1;
        }
        String logOnly = args.getString(ARGUMENT_LOG_ONLY);
        if (logOnly != null) {
            mLogOnly = Boolean.parseBoolean(logOnly);
        }
    }

    /**
     * Filter the alias-ed options from the bundle, each implementation of BaseMetricListener will
     * have its own list of arguments.
     * TODO: Split the filtering logic outside the collector class in a utility/helper.
     */
    private void filterAlias(Bundle bundle) {
        Set<String> keySet = new HashSet<>(bundle.keySet());
        OptionClass optionClass = this.getClass().getAnnotation(OptionClass.class);
        if (optionClass == null) {
            // No @OptionClass was specified, remove all alias-ed options.
            for (String key : keySet) {
                if (key.indexOf(NAMESPACE_SEPARATOR) != -1) {
                    bundle.remove(key);
                }
            }
            return;
        }
        // Alias is a required field so if OptionClass is set, alias is set.
        String alias = optionClass.alias();
        for (String key : keySet) {
            if (key.indexOf(NAMESPACE_SEPARATOR) == -1) {
                continue;
            }
            String optionAlias = key.split(NAMESPACE_SEPARATOR)[0];
            if (alias.equals(optionAlias)) {
                // Place the option again, without alias.
                String optionName = key.split(NAMESPACE_SEPARATOR)[1];
                bundle.putString(optionName, bundle.getString(key));
                bundle.remove(key);
            } else {
                // Remove other aliases.
                bundle.remove(key);
            }
        }
    }

    /**
     * Helper to decide whether the collector should run or not against the test case.
     *
     * @param desc The {@link Description} of the method.
     * @return True if the collector should run.
     */
    private boolean shouldRun(Description desc) {
        if (mLogOnly) {
            return false;
        }

        MetricOption annotation = desc.getAnnotation(MetricOption.class);
        List<String> groups = new ArrayList<>();
        if (annotation != null) {
            String group = annotation.group();
            groups.addAll(Arrays.asList(group.split(",")));
        }
        if (!mExcludeFilters.isEmpty()) {
            for (String group : groups) {
                // Exclude filters has priority, if any of the group is excluded, exclude the method
                if (mExcludeFilters.contains(group)) {
                    return false;
                }
            }
        }
        // If we have include filters, we can only run what's part of them.
        if (!mIncludeFilters.isEmpty()) {
            for (String group : groups) {
                if (mIncludeFilters.contains(group)) {
                    return true;
                }
            }
            // We have include filter and did not match them.
            return false;
        }

        // Skip metric collection if current iteration is lesser than or equal to
        // given skip until iteration count.
        // mTestIdInvocationCount uses 1 indexing.
        if (mTestIdInvocationCount.containsKey(desc.toString())
                && mTestIdInvocationCount.get(desc.toString()) <= mSkipMetricUntilIteration) {
            Log.i(getTag(), String.format("Skipping metric collection. Current iteration is %d."
                    + "Requested to skip metric until %d",
                    mTestIdInvocationCount.get(desc.toString()),
                    mSkipMetricUntilIteration));
            return false;
        }

        // Check for iteration interval metric collection criteria.
        if (mTestIdInvocationCount.containsKey(desc.toString())
                && (mTestIdInvocationCount.get(desc.toString()) % mCollectIterationInterval != 0)) {
            return false;
        }
        return true;
    }

    /**
     * Returns iteration number for the test or 0 if the test hasn't started executing yet
     */
    protected int getIteration(Description description) {
        for (Annotation annotation : description.getAnnotations()) {
            // If IterationMetadata annotation is present, return the iteration number from it
            // As likely the test was renamed, so counting using mTestIdInvocationCount won't work
            if (annotation instanceof IterationMetadata) {
                return ((IterationMetadata) annotation).getIteration();
            }
        }
        return mTestIdInvocationCount.getOrDefault(description.toString(), 0);
    }

    /**
     * Special metadata annotation object that could indicate the original iteration number.
     * It could be useful to retrieve the iteration number for a test if the test was renamed
     * to include iteration number in the name.
     */
    public static class IterationMetadata implements Annotation {

        private final int mIteration;

        public IterationMetadata(int iteration) {
            mIteration = iteration;
        }

        public int getIteration() {
            return mIteration;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return IterationMetadata.class;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IterationMetadata that)) return false;
            return mIteration == that.mIteration;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mIteration);
        }
    }
}
