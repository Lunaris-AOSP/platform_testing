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

package android.tools.device.apphelpers

import android.app.Instrumentation
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.IComponentNameMatcher
import androidx.test.platform.app.InstrumentationRegistry

/** Helper to launch the Gmail app (not compatible with AOSP) */
class GmailAppHelper
@JvmOverloads
constructor(
    instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    appName: String = "Gmail",
    appComponent: IComponentNameMatcher =
        ComponentNameMatcher(
            packageName = "com.google.android.gm",
            className = "com.google.android.gm.ConversationListActivityGmail",
        ),
) : StandardAppHelper(instrumentation, appName, appComponent)
