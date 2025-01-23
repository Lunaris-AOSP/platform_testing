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

    /** {@inheritDoc} */
    @Override
    public boolean isHunDisplayed() {
        UiObject2 headsUpNotification = findHun();
        return headsUpNotification != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmsHunDisplayedWithTitle(String text) {
        Log.i(LOG_TAG, String.format("Checking if SMS heads-up notification with title  %s is displayed in the car's head unit.", text));

        UiObject2 headsUpNotification = findHun();
        Log.i(LOG_TAG, "Heads-up notification object: " + headsUpNotification);

        while (headsUpNotification != null) {
            if (isHunTitleMatched(headsUpNotification, text)) {
                Log.i(LOG_TAG, "SMS heads-up notification displayed.");
                return true;
            }
            dismissHun(headsUpNotification);
            headsUpNotification = findHun();
        }

        Log.i(LOG_TAG, String.format("Cannot find SMS heads-up notification with title %s.", text));
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void playSmsHun() {
        Log.i(LOG_TAG, "Clicking on play button of SMS heads-up notification in the car's head unit.");
        UiObject2 headsUpNotification = findHun();
        UiObject2 playButton = headsUpNotification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_PLAY_BUTTON)
        );
        getSpectatioUiUtil().clickAndWait(playButton);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmsHunPlayedViaCarSpeaker() {
        Log.i(LOG_TAG, "Checking if SMS heads-up notification is played in the car's head unit.");
        // TODO: Implement this method. Need to verify if the sound is played from special channel.
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void muteSmsHun() {
        Log.i(LOG_TAG, "Clicking on mute button of SMS heads-up notification in the car's head unit.");
        UiObject2 headsUpNotification = findHun();
        Log.i(LOG_TAG, "Muting notification object: " + headsUpNotification);
        UiObject2 muteButton = headsUpNotification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_MUTE_BUTTON)
        );
        getSpectatioUiUtil().clickAndWait(muteButton, 2000);
    }

    /** {@inheritDoc} */
    @Override
    public void swipeHun() {
        Log.i(LOG_TAG, "Swiping the SMS heads-up notification in the car's head unit.");
        UiObject2 headsUpNotification = findHun();
        dismissHun(headsUpNotification);
    }

    /**
     * Find the heads-up notification in the car's head unit.
     *
     * @return The UiObject2 representing the heads-up notification, or null if it's not found.
     */
    private UiObject2 findHun() {
        Log.i(LOG_TAG, "Checking for heads-up notification in the car's head unit.");
        BySelector headsUpNotificationSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION);
        UiObject2 notification = getSpectatioUiUtil().waitForUiObject(headsUpNotificationSelector);

        if (notification == null) {
            Log.w(LOG_TAG, "Cannot to find heads-up notification in the car's head unit.");
        }

        return notification;
    }

    /**
     * Check if the heads-up notification title is matched with the given text.
     *
     * @param text The text to match with the heads-up notification title.
     * @return True if the heads-up notification title is matched with the given text, false
     * otherwise.
     */
    private boolean isHunTitleMatched(UiObject2 headsUpNotification, String text) {
        UiObject2 headsUpNotificationTitle = headsUpNotification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_TITLE)
        );
        if (headsUpNotificationTitle == null) {
            Log.w(LOG_TAG, "Cannot to find heads-up notification title in the car's head unit.");
            return false;
        }
        String titleText = headsUpNotificationTitle.getText().toLowerCase();
        return titleText != null && titleText.contains(text.toLowerCase());
    }

    /**
     * Dismiss via swipe any heads-up notification in the car's head unit.
     *
     * @param headsUpNotification The UiObject2 representing the heads-up notification.
     */
    private void dismissHun(UiObject2 headsUpNotification) {
        Log.i(LOG_TAG, "Dismissing the heads-up notification in the car's head unit.");
        getSpectatioUiUtil().swipeRight(headsUpNotification);
        // Wait for the notification to dismiss, if it takes more than 1 second to dismiss, it is a performance issue.
        getSpectatioUiUtil().waitNSeconds(2000);
    }

}
