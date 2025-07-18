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

package platform.test.runner.parameterized;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.rules.TestRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.Collections;
import java.util.List;

/** Parameterized runner for Android instrumentation. */
public class AndroidParameterizedRunner extends AndroidJUnit4ClassRunner {

    private final ParameterizedRunnerDelegate mDelegate;

    public AndroidParameterizedRunner(Class<?> type, int parametersIndex, String name)
            throws InitializationError {
        super(type);
        mDelegate = new ParameterizedRunnerDelegate(parametersIndex, name);
    }

    @Override
    protected String getName() {
        return mDelegate.getName();
    }

    @Override
    protected String testName(final FrameworkMethod method) {
        return method.getName() + getName();
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        validateOnlyOneConstructor(errors);
        if (ParameterizedRunnerDelegate.fieldsAreAnnotated(getTestClass())) {
            validateZeroArgConstructor(errors);
        }
    }

    @Override
    public String toString() {
        return "AndroidParameterizedRunner " + mDelegate.getName();
    }

    @Override
    protected void validateFields(List<Throwable> errors) {
        super.validateFields(errors);
        ParameterizedRunnerDelegate.validateFields(errors, getTestClass());
    }

    @Override
    protected Object createTest() throws Exception {
        return mDelegate.createTestInstance(getTestClass());
    }

    @Override
    protected List<TestRule> classRules() {
        // We are already running ClassRules at the top class level, don't run them again
        // on each parameter (b/377890695)
        return Collections.emptyList();
    }
}
