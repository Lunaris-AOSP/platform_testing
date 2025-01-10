/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.platform.helpers;

import android.util.Log;
import android.app.Instrumentation;
import android.platform.helpers.ScrollUtility.ScrollActions;
import android.platform.helpers.ScrollUtility.ScrollDirection;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

import java.util.List;
import java.lang.Exception;

/**
 * Helper for heads-up notifications on Automotive device.
 */
public class AutoHeadsUpNotificationHelperImpl extends AbstractStandardAppHelper
    implements IAutoHeadsUpNotificationHelper {

    private static final String LOG_TAG = AutoHeadsUpNotificationHelperImpl.class.getSimpleName();

    public AutoHeadsUpNotificationHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }

    private UiObject2 findHeadsUpNotification() {
        Log.i(LOG_TAG, "Checking for heads-up notification in the car's head unit.");
        BySelector headsUpNotificationSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION);

        try {
            return getSpectatioUiUtil().waitForUiObject(headsUpNotificationSelector);
        } catch (Exception e) {
            throw new RuntimeException("Heads-up notification not found in the car's head unit.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHUNDisplayed() {
        UiObject2 headsUpNotification = findHeadsUpNotification();
        return headsUpNotification != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSMSHUNWWithTitleDisplayed(String text) {
        Log.i(LOG_TAG, String.format("Checking if SMS heads-up notification with title  %s is displayed in the car's head unit.", text));
        UiObject2 headsUpNotification = findHeadsUpNotification();
        UiObject2 headsUpNotificationTitle = headsUpNotification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_TITLE)
        );
        Log.i(LOG_TAG, "Heads-up notification title: " + headsUpNotificationTitle);

        if (headsUpNotificationTitle != null) {
            String titleText = headsUpNotificationTitle.getText().toLowerCase();
            Log.i(LOG_TAG, "Heads-up notification title text: " + titleText);
            return titleText != null && titleText.contains(text.toLowerCase());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void playSMSHUN() {
        Log.i(LOG_TAG, "Clicking on play button of SMS heads-up notification in the car's head unit.");
        UiObject2 headsUpNotification = findHeadsUpNotification();
        UiObject2 playButton = headsUpNotification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_PLAY_BUTTON)
        );

        try {
            getSpectatioUiUtil().clickAndWait(playButton);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Failed to click on play button of SMS heads-up notification in the car's head unit.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSMSNUNPlayed() {
        Log.i(LOG_TAG, "Checking if SMS heads-up notification is played in the car's head unit.");
        // TODO: Implement this method. Need to verify if the sound is played from special channel.
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void muteSMSHUN() {
        Log.i(LOG_TAG, "Clicking on play button of SMS heads-up notification in the car's head unit.");
        UiObject2 headsUpNotification = findHeadsUpNotification();
        UiObject2 muteButton = headsUpNotification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_MUTE_BUTTON)
        );

        try {
            getSpectatioUiUtil().clickAndWait(muteButton);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Failed to click on mute button of SMS heads-up notification in the car's head unit.", e);
        }

        // Wait extra 1 second for the mute button to be disabled.
        // If it takes more time, then it is the performance issue.
        getSpectatioUiUtil().wait1Second();
    }
}
