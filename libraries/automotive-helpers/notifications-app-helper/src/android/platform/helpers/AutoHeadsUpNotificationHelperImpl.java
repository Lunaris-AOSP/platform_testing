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

    /** {@inheritDoc} */
    @Override
    public boolean isHUNDisplayed() {
        Log.i(LOG_TAG, "Checking if heads-up notification displayed in the car's head unit.");

        BySelector headsUpNotificationSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION);
        UiObject2 headsUpNotification = getSpectatioUiUtil().waitForUiObject(headsUpNotificationSelector);
        Log.i(LOG_TAG, "headsUpNotification: " + headsUpNotification);

        return headsUpNotification != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSMSHUNDisplayed(String phoneNumber) {
        Log.i(LOG_TAG, String.format("Checking if SMS heads-up notification from phone number %s is displayed in the car's head unit.", phoneNumber));

        BySelector headsUpNotificationTitleSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_TITLE);
        UiObject2 headsUpNotificationTitle = getSpectatioUiUtil().waitForUiObject(headsUpNotificationTitleSelector);

        if (headsUpNotificationTitle != null) {
            String titleText = headsUpNotificationTitle.getText(); // Assuming title contains phone number.
            Log.i(LOG_TAG, "Heads-up notification title text: " + titleText);
            return titleText != null && titleText.contains(phoneNumber);
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void playSMSHUN() {
        Log.i(LOG_TAG, "Clicking on play button of SMS heads-up notification in the car's head unit.");
        try {
            BySelector playButtonSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_PLAY_BUTTON);
            UiObject2 playButton = getSpectatioUiUtil().findUiObject(playButtonSelector);
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
        try {
            BySelector muteButtonSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_MUTE_BUTTON);
            UiObject2 muteButton = getSpectatioUiUtil().findUiObject(muteButtonSelector);
            getSpectatioUiUtil().clickAndWait(muteButton);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Failed to click on mute button of SMS heads-up notification in the car's head unit.", e);
        }
    }
}
