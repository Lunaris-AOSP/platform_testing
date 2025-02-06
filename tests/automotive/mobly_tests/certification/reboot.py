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

class AndroidRebootTest(base_test.BaseTestClass):

    def setup_class(self):
        # Setup required for the entire test class. This executes
        # before any test cases are run.
        self.android_devices = self.register_controller(android_device)

    def test_reboot_and_check_fingerprint(self):
        # Check that there is at least one device available.
        if not self.android_devices:
            raise AssertionError('No Android devices are registered. Please make '
                                 'sure at least one device is connected and '
                                 'configured properly.')

        ad = self.android_devices[0]  # Get the first device object.

        # Record the fingerprint before the reboot.
        before_reboot_fingerprint = ad.adb.shell('getprop ro.build.fingerprint')
        logging.info('Fingerprint before reboot: %s', before_reboot_fingerprint)

        # Reboot the device and wait until it's back online.
        ad.reboot()
        ad.wait_for_boot_completion()

        # Check the fingerprint after the reboot.
        after_reboot_fingerprint = ad.adb.shell('getprop ro.build.fingerprint')
        logging.info('Fingerprint after reboot: %s', after_reboot_fingerprint)

        # Verify that the fingerprint remains the same after the reboot
        if before_reboot_fingerprint != after_reboot_fingerprint:
            raise AssertionError('The ro.build.fingerprint has changed after '
                                 'reboot. Before: %s, after: %s' %
                                 (before_reboot_fingerprint, after_reboot_fingerprint))

if __name__ == '__main__':
    common_main()

