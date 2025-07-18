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
import android.graphics.Insets;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Trace;
import android.view.View;
import android.view.WindowInsets;

import com.android.performanceLaunch.helper.SimpleGLSurfaceView;

/**
 * To draw the GLSurface view Source : development/samples/OpenGL/HelloOpenGLES20
 */
public class SimpleSurfaceGLActivity extends Activity {

    private GLSurfaceView mGLView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("onCreate");
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        mGLView = new SimpleGLSurfaceView(this);
        setContentView(mGLView);
        if (mGLView.getParent() instanceof final View parentView) {
            parentView.setOnApplyWindowInsetsListener(
                    (view, windowInsets) -> {
                        final Insets insets = windowInsets.getSystemWindowInsets();
                        view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                        return WindowInsets.CONSUMED;
                    });
        }
        Trace.endSection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
    }
}
