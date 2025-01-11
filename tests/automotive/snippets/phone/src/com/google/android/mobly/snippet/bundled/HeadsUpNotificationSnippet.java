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

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoHeadsUpNotificationHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

/** Snippet for heads-up notification. */
public class HeadsUpNotificationSnippet implements Snippet {

    private final HelperAccessor<IAutoHeadsUpNotificationHelper> mHeadsUpNotificationHelper;

    public HeadsUpNotificationSnippet() {
        mHeadsUpNotificationHelper = new HelperAccessor<>(IAutoHeadsUpNotificationHelper.class);
    }

    @Rpc(description = "Check whether heads-up notification is displayed in the car's head unit.")
    public boolean isHUNDisplayed() {
        return mHeadsUpNotificationHelper.get().isHUNDisplayed();
    }

    @Rpc(description = "Check whether SMS heads-up notification from the given title is displayed in the car's head unit.")
    public boolean isSMSHUNWWithTitleDisplayed(String text) {
        return mHeadsUpNotificationHelper.get().isSMSHUNWWithTitleDisplayed(text);
    }

    @Rpc(description = "Play the SMS heads-up notification in the car's head unit.")
    public void playSMSHUN() {
        mHeadsUpNotificationHelper.get().playSMSHUN();
    }

    @Rpc(description = "Check if the SMS is played on the car's head unit via car's speaker.")
    public boolean isSMSNUNPlayed() {
        return mHeadsUpNotificationHelper.get().isSMSNUNPlayed();
    }

    @Rpc(description = "Mute the SMS heads-up notification in the car's head unit.")
    public void muteSMSHUN() {
        mHeadsUpNotificationHelper.get().muteSMSHUN();
    }

    @Rpc(description = "Swipe the SMS heads-up notification in the car's head unit.")
    public void swipeSMSHUN() {
        mHeadsUpNotificationHelper.get().swipeSMSHUN();
    }
}
