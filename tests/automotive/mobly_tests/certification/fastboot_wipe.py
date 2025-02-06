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

import logging
from mobly import base_test
from mobly import test_runner
from mobly.controllers import android_device
from utilities.main_utils import common_main

class AndroidFastbootWipeTest(base_test.BaseTestClass):

    def setup_class(self):
        self.android_devices = self.register_controller(android_device)

    def test_fastboot_wipe(self):
        # Check that there is at least one device available.
        if not self.android_devices:
            raise AssertionError('No Android devices are registered. Please make '
                                'sure at least one device is connected and '
                                'configured properly.')
        ad = self.android_devices[0]  # Get the first device object.

        # Reboot the device into fastboot mode using adb.
        logging.info('Rebooting device into fastboot mode.')
        ad.adb.reboot('bootloader')

        # Wait for the device to enter fastboot mode.
        ad.fastboot.wait_for_device()

        # Wipe device using fastboot.
        logging.info('Wiping device...')
        ad.fastboot.wipe()

        # Reboot the device after the wipe.
        logging.info('Rebooting device...')
        ad.fastboot.reboot()

        # Wait for the device to complete its booting process.
        ad.wait_for_boot_completion()

        # Check the status of the device.
        fingerprint = ad.adb.shell('getprop ro.build.fingerprint')
        logging.info('Build fingerprint: %s', fingerprint)
        if not fingerprint:
            raise AssertionError('After wiping and rebooting, the build fingerprint could not be found.')

        logging.info('Fastboot wipe test completed successfully.')


if __name__ == '__main__':
    common_main()
