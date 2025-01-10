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

public interface IAutoHeadsUpNotificationHelper extends Scrollable, IAppHelper {
    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * <p>Check whether HUN is displayed in the device.
     */
    boolean isHUNDisplayed();

    /**
     * Setup expectations: SMS is sent to the paired phone.
     *
     * <p>Check whether SMS HUN with the given phone number is displayed in car's head unit.
     *
     * @param text phone number of the sender of the SMS.
     */
    boolean isSMSHUNWWithTitleDisplayed(String text);

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * <p>Play the SMS HUN in the car's head unit.
     */
    void playSMSHUN();


    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * <p>Check whether SMS HUN is played in the car's head unit.
     */
    boolean isSMSNUNPlayed();

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * <p>Mute the SMS HUN in the car's head unit. If the new SMS is sent to the
     *  paired phone from the same sender, the new SMS HUN will not be displayed.
     */
    void muteSMSHUN();
}
