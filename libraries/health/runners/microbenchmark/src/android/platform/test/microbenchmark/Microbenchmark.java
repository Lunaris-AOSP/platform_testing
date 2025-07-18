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
package android.platform.test.microbenchmark;

import static android.content.Context.BATTERY_SERVICE;
import static android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY;
import static android.os.BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER;

import android.device.collectors.BaseMetricListener.IterationMetadata;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.platform.test.composer.Iterate;
import android.platform.test.rule.DynamicRuleChain;
import android.platform.test.rule.HandlesClassLevelExceptions;
import android.platform.test.rule.TracePointRule;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.test.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MemberValueConsumer;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code Microbenchmark} runner allows you to run individual JUnit {@code @Test} methods
 * repeatedly like a benchmark test in order to get more repeatable, reliable measurements, or to
 * ensure it passes when run many times. This runner supports a number of customized features that
 * enable better benchmarking and better integration with surrounding tools.
 *
 * <p>One iteration represents a single pass of a test method. The number of iterations and how
 * those iterations get renamed are configurable with options listed at the top of the source code.
 * Some custom annotations also exist to enable this runner to replicate JUnit's functionality with
 * modified behaviors related to running repeated methods. These are documented separately below
 * with the corresponding annotations, {@link @NoMetricBefore}, {@link @NoMetricAfter}, and
 * {@link @TightMethodRule}.
 *
 * The order of the execution for 2 iterations of a test with single test method:
 *  - @ClassRule before
 *      *iteration 1*
 *      - @NoMetricRule before
 *      - @NoMetricBefore method
 *          - *starts tracing*
 *              - @Rule before
 *              - @Before method
 *              - @TightMethodRule before
 *                  - Test method body
 *              - @TightMethodRule after
 *              - @After method
 *              - @Rule after
 *          - *ends tracing*
 *      - @NoMetricAfter method
 *      - @NoMetricRule after
 *      *iteration 2*
 *      - @NoMetricRule before
 *      - @NoMetricBefore method
 *          - *starts tracing*
 *              - @Rule before
 *              - @Before method
 *              - @TightMethodRule before
 *                  - Test method body
 *              - @TightMethodRule after
 *              - @After method
 *              - @Rule after
 *          - *ends tracing*
 *      - @NoMetricAfter method
 *      - @NoMetricRule after
 *   - @ClassRule after
 *
 * Note: the order of the execution is different from Functional runner.
 * See documentation for {@link Functional} for details.
 *
 * <p>Finally, this runner supports some power-specific testing features used to denoise (also
 * documented below), and can be configured to terminate early if the battery drops too low or if
 * any test fails.
 */
@HandlesClassLevelExceptions
public class Microbenchmark extends BlockJUnit4ClassRunner {

    private static final String LOG_TAG = Microbenchmark.class.getSimpleName();

    private static final Statement EMPTY_STATEMENT =
            new Statement() {
                @Override
                public void evaluate() throws Throwable {}
            };

    // Use these options to inject rules at runtime via the command line. For details, please see
    // documentation for DynamicRuleChain.
    @VisibleForTesting static final String DYNAMIC_OUTER_CLASS_RULES_OPTION = "outer-class-rules";
    @VisibleForTesting static final String DYNAMIC_INNER_CLASS_RULES_OPTION = "inner-class-rules";
    @VisibleForTesting static final String DYNAMIC_OUTER_TEST_RULES_OPTION = "outer-test-rules";
    @VisibleForTesting static final String DYNAMIC_INNER_TEST_RULES_OPTION = "inner-test-rules";

    // Renames repeated test methods as <description><separator><iteration> (if set to true).
    public static final String RENAME_ITERATION_OPTION = "rename-iterations";
    @VisibleForTesting static final String ITERATION_SEP_OPTION = "iteration-separator";
    @VisibleForTesting static final String ITERATION_SEP_DEFAULT = "$";

    // Stop running tests after any failure is encountered (if set to true).
    private static final String TERMINATE_ON_TEST_FAIL_OPTION = "terminate-on-test-fail";

    // Don't start new iterations if the battery falls below this value (if set).
    @VisibleForTesting static final String MIN_BATTERY_LEVEL_OPTION = "min-battery";
    // Don't start new iterations if the battery already fell more than this value (if set).
    @VisibleForTesting static final String MAX_BATTERY_DRAIN_OPTION = "max-battery-drain";
    // Options for aligning with the battery charge (coulomb) counter for power tests. We want to
    // start microbenchmarks just after the coulomb counter has decremented to account for the
    // counter being quantized. The counter most accurately reflects the true value just after it
    // decrements.
    private static final String ALIGN_WITH_CHARGE_COUNTER_OPTION = "align-with-charge-counter";
    private static final String COUNTER_DECREMENT_TIMEOUT_OPTION = "counter-decrement-timeout_ms";

    private final String mIterationSep;
    private final Bundle mArguments;
    private final boolean mRenameIterations;
    private final int mMinBatteryLevel;
    private final int mMaxBatteryDrain;
    private final int mCounterDecrementTimeoutMs;
    private final boolean mAlignWithChargeCounter;
    private final boolean mTerminateOnTestFailure;
    private final Map<Description, Integer> mIterations = new HashMap<>();
    private int mStartBatteryLevel;

    private final BatteryManager mBatteryManager;

    /**
     * Called reflectively on classes annotated with {@code @RunWith(Microbenchmark.class)}.
     */
    public Microbenchmark(Class<?> klass) throws InitializationError {
        this(klass, InstrumentationRegistry.getArguments());
    }

    /** Do not call. Called explicitly from tests to provide an arguments. */
    @VisibleForTesting
    Microbenchmark(Class<?> klass, Bundle arguments) throws InitializationError {
        super(klass);
        mArguments = arguments;
        // Parse out additional options.
        mRenameIterations = Boolean.parseBoolean(arguments.getString(RENAME_ITERATION_OPTION));
        mIterationSep =
                arguments.containsKey(ITERATION_SEP_OPTION)
                        ? arguments.getString(ITERATION_SEP_OPTION)
                        : ITERATION_SEP_DEFAULT;
        mMinBatteryLevel = Integer.parseInt(arguments.getString(MIN_BATTERY_LEVEL_OPTION, "-1"));
        mMaxBatteryDrain = Integer.parseInt(arguments.getString(MAX_BATTERY_DRAIN_OPTION, "100"));
        mCounterDecrementTimeoutMs =
                Integer.parseInt(arguments.getString(COUNTER_DECREMENT_TIMEOUT_OPTION, "30000"));
        mAlignWithChargeCounter =
                Boolean.parseBoolean(
                        arguments.getString(ALIGN_WITH_CHARGE_COUNTER_OPTION, "false"));

        mTerminateOnTestFailure =
                Boolean.parseBoolean(
                        arguments.getString(TERMINATE_ON_TEST_FAIL_OPTION, "false"));

        // Get the battery manager for later use.
        mBatteryManager =
                (BatteryManager)
                        InstrumentationRegistry.getContext().getSystemService(BATTERY_SERVICE);
    }

    @Override
    public void run(final RunNotifier notifier) {
        if (mAlignWithChargeCounter) {
            // Try to wait until the coulomb counter has just decremented to start the test.
            int startChargeCounter = getBatteryChargeCounter();
            long startTimestamp = SystemClock.uptimeMillis();
            while (startChargeCounter == getBatteryChargeCounter()) {
                if (SystemClock.uptimeMillis() - startTimestamp > mCounterDecrementTimeoutMs) {
                    Log.d(
                            LOG_TAG,
                            "Timed out waiting for the counter to change. Continuing anyway.");
                    break;
                } else {
                    Log.d(
                            LOG_TAG,
                            String.format(
                                    "Charge counter still reads: %d. Waiting.",
                                    startChargeCounter));
                    SystemClock.sleep(getCounterPollingInterval());
                }
            }
        }
        Log.d(LOG_TAG, String.format("The charge counter reads: %d.", getBatteryChargeCounter()));

        mStartBatteryLevel = getBatteryLevel();

        super.run(notifier);
    }

    /**
     * Returns a {@link Statement} that invokes {@code method} on {@code test}, surrounded by any
     * explicit or command-line-supplied {@link TightMethodRule}s. This allows for tighter {@link
     * TestRule}s that live inside {@link Before} and {@link After} statements.
     */
    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        Statement start = super.methodInvoker(method, test);
        // Wrap the multiple-iteration test method with trace points.
        start = getTracePointRule().apply(start, describeChild(method));
        // Invoke special @TightMethodRules that wrap @Test methods.
        List<TestRule> tightMethodRules =
                getTestClass().getAnnotatedFieldValues(test, TightMethodRule.class, TestRule.class);
        for (TestRule tightMethodRule : tightMethodRules) {
            start = tightMethodRule.apply(start, describeChild(method));
        }
        return start;
    }

    @VisibleForTesting
    protected TracePointRule getTracePointRule() {
        return new TracePointRule();
    }

    /**
     * Returns a list of repeated {@link FrameworkMethod}s to execute.
     */
    @Override
    protected List<FrameworkMethod> getChildren() {
       return new Iterate<FrameworkMethod>().apply(mArguments, super.getChildren());
    }

    /**
     * An annotation for the corresponding tight rules above. These rules are ordered differently
     * from standard JUnit {@link Rule}s because they live between {@link Before} and {@link After}
     * methods, instead of wrapping those methods.
     *
     * <p>In particular, these serve as a proxy for tight metric collection in microbenchmark-style
     * tests, where collection is isolated to just the method under test. This is important for when
     * {@link Before} and {@link After} methods will obscure signal reliability.
     *
     * <p>Currently these are only registered from inside a test class as follows, but should soon
     * be extended for command-line support.
     *
     * ```
     * @RunWith(Microbenchmark.class)
     * public class TestClass {
     *     @TightMethodRule
     *     public ExampleRule exampleRule = new ExampleRule();
     *
     *     @Test ...
     * }
     * ```
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface TightMethodRule {}

    /**
     * A temporary annotation that acts like the {@code @Before} but is excluded from metric
     * collection.
     *
     * <p>This should be removed as soon as possible. Do not use this unless explicitly instructed
     * to do so. You'll regret it!
     *
     * <p>Note that all {@code TestOption}s must be instantiated as {@code @ClassRule}s to work
     * inside these annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface NoMetricBefore {}

    /** A temporary annotation, same as the above, but for replacing {@code @After} methods. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface NoMetricAfter {}

    /** A temporary annotation, same as the above, but for replacing JUnit {@code @Rule}. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface NoMetricRule {}

    /**
     * Rename the child class name to add iterations if the renaming iteration option is enabled.
     *
     * <p>Renaming the class here is chosen over renaming the method name because
     *
     * <ul>
     *   <li>Conceptually, the runner is running a class multiple times, as opposed to a method.
     *   <li>When instrumenting a suite in command line, by default the instrumentation command
     *       outputs the class name only. Renaming the class helps with interpretation in this case.
     */
    @Override
    protected Description describeChild(FrameworkMethod method) {
        Description original = super.describeChild(method);
        if (!mRenameIterations) {
            return original;
        }

        final int iteration = mIterations.getOrDefault(original, 0);
        final List<Annotation> annotations = new ArrayList<>(original.getAnnotations());
        annotations.add(new IterationMetadata(iteration));

        return Description.createTestDescription(
                original.getTestClass(),
                String.join(
                        mIterationSep,
                        original.getMethodName(),
                        String.valueOf(iteration)),
                annotations.toArray(Annotation[]::new));
    }

    /** Re-implement the private rules wrapper from {@link BlockJUnit4ClassRunner} in JUnit 4.12. */
    private Statement withRules(FrameworkMethod method, Object target, Statement statement) {
        Statement result = statement;
        List<TestRule> testRules = new ArrayList<>();
        // Inner dynamic rules should be included first because RunRules applies rules inside-out.
        testRules.add(new DynamicRuleChain(DYNAMIC_INNER_TEST_RULES_OPTION, mArguments));
        testRules.addAll(getTestRules(target));
        testRules.add(new DynamicRuleChain(DYNAMIC_OUTER_TEST_RULES_OPTION, mArguments));
        // Apply legacy MethodRules, if they don't overlap with TestRules.
        for (org.junit.rules.MethodRule each : rules(target)) {
            if (!testRules.contains(each)) {
                result = each.apply(result, method, target);
            }
        }
        // Apply modern, method-level TestRules in outer statements.
        result = new RunRules(result, testRules, describeChild(method));
        return result;
    }

    /**
     * @param target the test case instance
     * @return a list of NoMetricTestRules that should be applied when executing this
     *         test
     */
    private List<TestRule> getNoMetricTestRules(Object target) {
        final List<TestRule> result = new ArrayList<>();
        final MemberValueConsumer<TestRule> collector = (member, value) -> result.add(value);

        getTestClass().collectAnnotatedMethodValues(target, NoMetricRule.class, TestRule.class,
                collector);
        getTestClass().collectAnnotatedFieldValues(target, NoMetricRule.class, TestRule.class,
                collector);

        return result;
    }

    /** Add {@link DynamicRuleChain} to existing class rules. */
    @Override
    protected List<TestRule> classRules() {
        List<TestRule> classRules = new ArrayList<>();
        // Inner dynamic class rules should be included first because RunRules applies rules inside
        // -out.
        classRules.add(new DynamicRuleChain(DYNAMIC_INNER_CLASS_RULES_OPTION, mArguments));
        classRules.addAll(super.classRules());
        classRules.add(new DynamicRuleChain(DYNAMIC_OUTER_CLASS_RULES_OPTION, mArguments));
        return classRules;
    }

    /**
     * Combine the {@code #runChild}, {@code #methodBlock}, and final {@code #runLeaf} methods to
     * implement the specific {@code Microbenchmark} test behavior. In particular, (1) keep track of
     * the number of iterations for a particular method description, and (2) run {@code
     * NoMetricBefore} and {@code NoMetricAfter} methods outside of the {@code RunListener} test
     * wrapping methods.
     */
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (isBatteryLevelBelowMin()) {
            throw new TerminateEarlyException("the battery level is below the threshold.");
        } else if (isBatteryDrainAboveMax()) {
            throw new TerminateEarlyException("the battery drain is above the threshold.");
        }

        // Update the number of iterations this method has been run.
        if (mRenameIterations) {
            Description original = super.describeChild(method);
            mIterations.computeIfPresent(original, (k, v) -> v + 1);
            mIterations.computeIfAbsent(original, k -> 1);
        }

        Description description = describeChild(method);
        if (isIgnored(method)) {
            notifier.fireTestIgnored(description);
        } else {
            EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);

            Object test;
            try {
                // Fail fast if the test is not successfully created.
                test =
                        new ReflectiveCallable() {
                            @Override
                            protected Object runReflectiveCall() throws Throwable {
                                return createTest();
                            }
                        }.run();
            } catch (Throwable e) {
                eachNotifier.fireTestStarted();
                eachNotifier.addFailure(e);
                eachNotifier.fireTestFinished();
                if (mTerminateOnTestFailure) {
                    throw new TerminateEarlyException("test failed.");
                }
                return;
            }

            Statement statement = methodInvoker(method, test);
            statement = possiblyExpectingExceptions(method, test, statement);
            statement = withPotentialTimeout(method, test, statement);
            statement = withBefores(method, test, statement);
            statement = withAfters(method, test, statement);
            statement = withRules(method, test, statement);

            // Fire test events from inside to exclude "no metric" methods.
            statement = withTestEventsNotifier(eachNotifier, statement);

            statement = withNoMetricsAfters(eachNotifier, test, statement);
            statement = withNoMetricsBefores(eachNotifier, test, statement);
            statement = withNoMetricsRules(method, test, statement);

            boolean testFailed = false;

            try {
                statement.evaluate();
            } catch (Throwable e) {
                testFailed = true;
            }

            if (mTerminateOnTestFailure && testFailed) {
                throw new TerminateEarlyException("test failed.");
            }
        }
    }

    private Statement withNoMetricsBefores(EachTestNotifier eachNotifier, Object test,
            Statement next) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    for (FrameworkMethod noMetricBefore :
                            getTestClass().getAnnotatedMethods(NoMetricBefore.class)) {
                        noMetricBefore.invokeExplosively(test);
                    }
                } catch (Throwable e) {
                    eachNotifier.fireTestStarted();
                    eachNotifier.addFailure(e);
                    eachNotifier.fireTestFinished();
                    throw e;
                }

                next.evaluate();
            }
        };
    }

    private Statement withNoMetricsAfters(EachTestNotifier eachNotifier, Object test,
            Statement next) {
        final List<FrameworkMethod> afters = getTestClass()
                .getAnnotatedMethods(NoMetricAfter.class);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final List<Throwable> errors = new ArrayList<Throwable>();
                try {
                    next.evaluate();
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    for (FrameworkMethod each : afters) {
                        try {
                            each.invokeExplosively(test);
                        } catch (AssumptionViolatedException e) {
                            eachNotifier.addFailedAssumption(e);
                            errors.add(e);
                        } catch (Throwable e) {
                            eachNotifier.addFailure(e);
                            errors.add(e);
                        }
                    }
                }
                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    private Statement withTestEventsNotifier(EachTestNotifier eachNotifier, Statement next) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                eachNotifier.fireTestStarted();
                try {
                    next.evaluate();
                } catch (AssumptionViolatedException e) {
                    eachNotifier.addFailedAssumption(e);
                    throw e;
                } catch (Throwable e) {
                    eachNotifier.addFailure(e);
                    throw e;
                } finally {
                    eachNotifier.fireTestFinished();
                }
            }
        };
    }

    private Statement withNoMetricsRules(FrameworkMethod method, Object target,
            Statement next) {
        return new RunRules(next, getNoMetricTestRules(target), describeChild(method));
    }

    /* Checks if the battery level is below the specified level where the test should terminate. */
    private boolean isBatteryLevelBelowMin() {
        return getBatteryLevel() < mMinBatteryLevel;
    }

    /* Checks if the battery level has drained enough to where the test should terminate. */
    private boolean isBatteryDrainAboveMax() {
        return mStartBatteryLevel - getBatteryLevel() > mMaxBatteryDrain;
    }

    /* Gets the current battery level (as a percentage). */
    @VisibleForTesting
    public int getBatteryLevel() {
        return mBatteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY);
    }

    /* Gets the current battery charge counter (coulomb counter). */
    @VisibleForTesting
    public int getBatteryChargeCounter() {
        return mBatteryManager.getIntProperty(BATTERY_PROPERTY_CHARGE_COUNTER);
    }

    /* Gets the polling interval to check for changes in the battery charge counter. */
    @VisibleForTesting
    public long getCounterPollingInterval() {
        return 100;
    }

    /**
     * A {@code RuntimeException} class for terminating test runs early for some specified reason.
     */
    @VisibleForTesting
    static class TerminateEarlyException extends RuntimeException {
        public TerminateEarlyException(String message) {
            super(String.format("Terminating early because %s", message));
        }
    }

}
