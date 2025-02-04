/*
 * Copyright (C) 2025 The Android Open Source Project
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
package android.platform.spectatio.configs;

import com.google.gson.annotations.SerializedName;

/** Configuration specific to validate-value workflow tasks */
public class ValidationConfig {
    @SerializedName("PROPERTY")
    private String mProperty;

    @SerializedName("EXPECTED")
    private String mExpected;

    @SerializedName("EXPECTED_COMMAND_LINE_KEY")
    private String mExpectedCommandLineKey;

    public String getProperty() {
        return mProperty;
    }

    public String getExpected() {
        return mExpected;
    }

    public String getExpectedCommandLineKey() {
        return mExpectedCommandLineKey;
    }
}
