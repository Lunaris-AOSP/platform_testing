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
import os
import subprocess
from mobly import base_test
from mobly import test_runner
from mobly import config_parser
from mobly.controllers import android_device
from devices import DeviceFactory
from pathlib import Path
from utilities.main_utils import common_main

class AndroidFlashTest(base_test.BaseTestClass):

    def setup_class(self):
        # Setup required for the entire test class.
        logging.info('Setting up test class.')
        # Registers the android devices controllers with Mobly.
        self.android_devices = self.register_controller(android_device)
        # Retrieves the build path from the test configuration.
        self.device = self.user_params['device']
        self.serial = self.user_params['serial']
        self.image_dir = Path(self.user_params['image_dir'])
        self.wipe = self.user_params['wipe']
        self.hw_variant = self.user_params['hw_variant']
        self.b = self.user_params['b']
        self.diag = self.user_params['diag']
        self.preserve_keys = self.user_params['preserve_keys']
        self.total_flashes = self.user_params['total_flashes']

    def test_flash_device_stability(self):
        # Check that there is at least one device available.
        if not self.android_devices:
            raise AssertionError('No Android devices are registered. Please make '
                                 'sure at least one device is connected and '
                                 'configured properly.')
        ad = self.android_devices[0]  # Get the first device object.

        total_flashes = self.total_flashes 
        successful_flashes = 0

        for i in range(total_flashes):
            logging.info('Flash attempt #%d', i + 1)

            # Get the build fingerprint before flashing.
            before_flash_fingerprint = ad.adb.getprop('ro.build.fingerprint')

            try:

                Device = DeviceFactory.get_device(self.device)
                flash_result = Device.flash(self.serial, self.image_dir, self.wipe, self.hw_variant, self.b, self.diag, self.preserve_keys)
                # Reboot the device and wait for it to come back online.
                ad.reboot()
                ad.wait_for_boot_completion()

                # Get the build fingerprint after flashing.
                after_flash_fingerprint = ad.adb.getprop('ro.build.fingerprint')

                # Check if the flash was successful by comparing fingerprints.
                if before_flash_fingerprint == after_flash_fingerprint:
                    successful_flashes += 1
                else:
                    logging.error('Flash attempt #%d failed: fingerprint did not change.', i + 1)

            except subprocess.CalledProcessError as e:
                logging.error('Flash attempt #%d failed with return code %s', i + 1, e.returncode)

            # It's a good idea to have some delay between flashes to avoid overloading the device.
            # You can add sleep here if needed.

        # After the loop, log the results of the stability test.
        logging.info('Completed %d flash attempts with %d successes.', total_flashes, successful_flashes)

        # Optionally, assert that all flashes were successful to mark the test as passed.
        assert successful_flashes == total_flashes, 'Not all flash attempts were successful.'

if __name__ == '__main__':
    common_main()

