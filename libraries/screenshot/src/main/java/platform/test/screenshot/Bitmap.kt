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

package platform.test.screenshot

import platform.test.screenshot.matchers.MSSIMMatcher
import platform.test.screenshot.matchers.PixelPerfectMatcher

/**
 * The [BitmapMatcher][platform.test.screenshot.matchers.BitmapMatcher] that should be used for
 * screenshot *unit* tests.
 */
val UnitTestBitmapMatcher =
    if (System.getProperty("java.vm.name")?.equals("Dalvik") == false) { // isRobolectric
        // Different CPU architectures can sometimes end up rendering differently, so we can't do
        // pixel-perfect matching on different architectures using the same golden. Given that our
        // presubmits are run on cf_x86_64_phone, our goldens should be perfectly matched on the
        // x86_64 architecture and use the Structural Similarity Index on others.
        // TODO(b/237511747): Run our screenshot presubmit tests on arm64 instead so that we can
        // do pixel perfect matching both at presubmit time and at development time with actual
        // devices.
        PixelPerfectMatcher()
    } else {
        MSSIMMatcher()
    }

/**
 * The [BitmapMatcher][platform.test.screenshot.matchers.BitmapMatcher] that should be used for
 * screenshot *unit* tests.
 *
 * We use the Structural Similarity Index for integration tests because they usually contain
 * additional information and noise that shouldn't break the test.
 */
val IntegrationTestBitmapMatcher = MSSIMMatcher()
