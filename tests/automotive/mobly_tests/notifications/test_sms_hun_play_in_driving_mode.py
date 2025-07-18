#  Copyright (C) 2025 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging

from bluetooth_sms_test import bluetooth_sms_base_test
from mobly.controllers import android_device
from utilities import constants
from utilities.common_utils import CommonUtils
from utilities.faker import fake
from utilities.main_utils import common_main


class NotificationsSmsHunPlayInDrivingMode(
    bluetooth_sms_base_test.BluetoothSMSBaseTest
):

  def setup_class(self):
    super().setup_class()
    self.common_utils = CommonUtils(self.target, self.discoverer)

  def setup_test(self):
    logging.info("Pairing phone to car head unit.")
    self.bt_utils.pair_primary_to_secondary()
    self.call_utils.wait_with_log(constants.DEVICE_CONNECT_WAIT_TIME)
    self.common_utils.click_on_ui_element_with_text("Allow")

    logging.info("Clearing the sms from the phone.")
    self.call_utils.clear_sms_app(self.target)

    logging.info("Enabling driving mode.")
    self.call_utils.enable_driving_mode()

    logging.info("Removing mbs snippet and rebooting the phone.")
    self.target.unload_snippet('mbs')
    self.target.reboot()
    self.call_utils.wait_with_log(constants.DEVICE_CONNECT_WAIT_TIME)
    self.target.load_snippet('mbs', android_device.MBS_PACKAGE)
    super().enable_recording()

  def test_sms_hun_play_in_driving_mode(self):
    """
    GIVEN the phone which is paired to the car,
    WHEN the SMS is sent to paired phone,
    THEN the SMS appears as a heads-up notification on the car's head unit,
    WHEN the SMS is played,
    THEN the SMS is played on the car's head unit via car's speaker.
    """

    logging.info("Arrange: Get the phone number of phone to send the SMS.")
    receiver_phone_number = self.target.mbs.getPhoneNumber()
    sender_phone_number = self.phone_notpaired.mbs.getPhoneNumber()
    sms_text = fake.string

    logging.info(f"Act: Sending new SMS to {receiver_phone_number}")
    self.phone_notpaired.mbs.sendSms(receiver_phone_number, sms_text)

    logging.info("Assert: New SMS is displayed as a heads-up notification.")
    assert self.discoverer.mbs.isSmsHunDisplayed() is True, (
        "New SMS is not displayed as a heads-up notification."
    )
    assert self.discoverer.mbs.isSmsHunDisplayedWithTitle(sender_phone_number) is True, (
        "New SMS is not displayed as a heads-up notification with the correct title."
    )

    logging.info("Act: Playing the SMS in the car's head unit.")
    self.discoverer.mbs.playSmsHun(sender_phone_number)

    logging.info("Assert: SMS is played on the car's head unit via car's speaker.")
    assert self.discoverer.mbs.isSmsHunPlayedViaCarSpeaker() is True, (
        "SMS is not played on the car's head unit via car's speaker."
    )

  def teardown_test(self):
    self.call_utils.press_home()

    try:
      self.call_utils.disable_driving_mode()
    except Exception as e:  # pylint: disable=broad-except
      logging.info("Failed to disable driving mode: %s", e)

    try:
      super().teardown_test()
    except Exception as e:  # pylint: disable=broad-except
      logging.info("Failed to teardown test: %s", e)


if __name__ == "__main__":
  common_main()
