/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.performanceLaunch;


import android.app.Activity;
import android.os.Bundle;
import android.os.Trace;

public class ComplexLayoutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complex);
        Trace.endSection();
    }

}
