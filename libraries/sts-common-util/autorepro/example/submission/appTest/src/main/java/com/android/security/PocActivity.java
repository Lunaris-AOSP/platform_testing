/*
 * Copyright 2022 The Android Open Source Project
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
package com.android.security;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class PocActivity extends Activity {
    private static final String TAG = PocActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "poc activity started");

        // Collect the artifact representing vulnerability here.
        // Change this to whatever type best fits the vulnerable artifact; consider using a bundle
        // if there are multiple artifacts necessary to prove the security vulnerability.
        String artifact = "vulnerable";

        Intent vulnerabilityDescriptionIntent = new Intent(DeviceTest.ACTION_BROADCAST);
        vulnerabilityDescriptionIntent.putExtra(DeviceTest.INTENT_ARTIFACT, artifact);
        this.sendBroadcast(vulnerabilityDescriptionIntent);
    }
}
