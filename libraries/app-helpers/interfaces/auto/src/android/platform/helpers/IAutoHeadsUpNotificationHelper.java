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

public interface IAutoHeadsUpNotificationHelper extends Scrollable, IAppHelper {
    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * <p>Check whether HUN is displayed in the device.
     *
     * @return True if the HUN is displayed, false otherwise.
     */
    boolean isHunDisplayed();

    /**
     * Setup expectations: SMS is sent to the paired phone.
     *
     * <p>Check whether SMS HUN with the given tile is displayed in car's head unit.
     * <p>Swipe's the system notification bar to dismiss the HUN.
     *
     * @param text title (phone number in most cases) of the sender of the SMS.
     *
     * @return True if the SMS HUN is displayed with the given title, false otherwise.
     */
    boolean isSmsHunDisplayedWithTitle(String text);

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * <p>Play the SMS HUN in the car's head unit.
     */
    void playSmsHun();

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * <p>Check whether SMS HUN is played in the car's head unit.
     *
     * @return True if the SMS HUN is played in the car's head unit, false otherwise.
     */
    boolean isSmsHunPlayedViaCarSpeaker();

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * <p>Mute the SMS HUN in the car's head unit. If the new SMS is sent to the
     *  paired phone from the same sender, the new SMS HUN will not be displayed.
     */
    void muteSmsHun();

    /**
     * Setup expectations: Heads-up notification is posted.
     *
     * <p>Swipe top (first) heads-up notification in the car's head unit.
     */
    void swipeHun();
}
