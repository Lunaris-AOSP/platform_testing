"""Bluetooth ThreeDeviceTestBed Base Test

This test class serves as a base class for tests which needs three devices
"""

import logging

from bluetooth_test import bluetooth_base_test
from mobly.controllers import android_device
from utilities import bt_utils
from utilities import spectatio_utils
from utilities.main_utils import common_main
from utilities.video_utils_service import VideoRecording


class BluetoothSMSBaseTest(bluetooth_base_test.BluetoothBaseTest):

  def setup_class(self):
    # Registering android_device controller module and declaring the three devices.
    self.ads = self.register_controller(android_device, min_number=3)

    # The dicoverers is the car head unit.
    self.discoverer = android_device.get_device(self.ads, label='auto')
    self.discoverer.debug_tag = 'discoverer'
    self.discoverer.load_snippet('mbs', android_device.MBS_PACKAGE)

    # The phone device used to connect to the car.
    self.target = android_device.get_device(self.ads, label='phone')
    self.target.debug_tag = 'target'
    self.target.load_snippet('mbs', android_device.MBS_PACKAGE)

    # The extra phone device used to perform actions (make calls, send SMS).
    self.phone_notpaired = android_device.get_device(
        self.ads,
        label='phone_notpaired',
    )
    self.phone_notpaired.debug_tag = 'phone_notpaired'
    self.phone_notpaired.load_snippet('mbs', android_device.MBS_PACKAGE)

    self.call_utils = spectatio_utils.CallUtils(self.discoverer)
    self.bt_utils = bt_utils.BTUtils(self.discoverer, self.target)

    logging.info('Initializing video services')
    self.video_utils_service = VideoRecording(
        self.discoverer,
        self.__class__.__name__,
    )
    self.video_utils_service_target = VideoRecording(
        self.target,
        self.__class__.__name__,
    )

    logging.info('Enabling video recording for Discoverer device')
    self.video_utils_service.enable_screen_recording()

    logging.info('Enabling video recording for Target device')
    self.video_utils_service_target.enable_screen_recording()

    self.call_utils.press_phone_home_icon_using_adb_command(
        self.phone_notpaired
    )


if __name__ == '__main__':
  common_main()
