/*
 * Copyright (C) 2019 The Android Open Source Project
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

public interface IAutoNotificationHelper extends Scrollable, IAppHelper {
    /**
     * Setup expectations: Notification app is open and scrolled to the bottom.
     *
     * <p>Tap clear all button if present.
     */
    void tapClearAllBtn();

    /**
     * Setup expectations: A notification is received.
     *
     * <p>Check whether notification has been posted.
     *
     * @param title of the notification to be checked.
    */
    boolean checkNotificationExists(String title);

    /**
     * Setup expectations: A notification is received.
     *
     * <p>Check whether notification with specific title exists in notification center.
     *
     * @param title text of the notification.
     */
    boolean isNotificationDisplayedInCenterWithTitle(String title);

    /**
     * Setup expectations: A notification is received.
     *
     * <p>Check whether notification with specific content exists in notification center.
     *
     * @param content of the notification to be checked.
     */
    boolean isNotificationDisplayedInCenterWithContent(String content);

    /**
     * Setup expectations: A notification is received.
     *
     * <p>Swipe away a received notification to remove.
     *
     * @param title of the notification to be swiped.
     */
    void removeNotification(String title);

    /**
     * Setup expectations: Notification app is open and scrolled to the bottom.
     *
     * <p>Tap manange button if present.
     */
    void clickManageBtn();

    /**
     * Setup expectations: None.
     *
     * <p>Checks if notification settings page is opened.
     */
    boolean isNotificationSettingsOpened();

    /**
     * Setup expectations: None.
     *
     * <p>Checks if notification are received under recent category.
     */
    boolean isRecentNotification();

    /**
     * Setup expectations: None.
     *
     * <p>Checks if notification are received under older category.
     */
    boolean isOlderNotification();

    /**
     * Setup expectations: A notification is received.
     *
     * <p>Clicks in check recent permissions
     */
    void clickOnCheckRecentPermissions(String title);

    /**
     * Setup expectations: Check recent permission is launched.
     *
     * <p>Checks App Permissions is present
     */
    boolean checkAppPermissionsExists(String title);
}
