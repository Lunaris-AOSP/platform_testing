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

class WifiValidationTest(base_test.BaseTestClass):

    def setup_class(self):
        self.android_devices = self.register_controller(android_device)

    def wifi_toggle(self, enable):
        """Enables or disables the Wi-Fi on the device.

        Args:
            enable (bool): True to enable Wi-Fi, False to disable.
        """
        state = 'enable' if enable else 'disable'
        cmd = f'svc wifi {state}'
        output = self.android_devices[0].adb.shell(cmd)
        logging.info(f'Wi-Fi {state} command output: {output}')

    def check_wifi_status(self, expected_status):
        """Checks the current Wi-Fi status against the expected status.

        Args:
            expected_status (str): The expected Wi-Fi status, 'enabled' or 'disabled'.

        Returns:
            bool: True if the actual and expected statuses match, False otherwise.
        """
        status_output_bytes = self.android_devices[0].adb.shell('dumpsys wifi | grep "Wi-Fi is"')
        # Decode the bytes object to a string using UTF-8 or appropriate encoding
        status_output = status_output_bytes.decode('utf-8')
        logging.info(f'Current Wi-Fi status: {status_output}')
        # Comparing only the tail part of the line to have a match like 'Wi-Fi is enabled/disabled'
        return expected_status in status_output.split()[-1]

    def test_wifi_validation(self):
        # Ensure at least one device is available
        if not self.android_devices:
            raise AssertionError('No Android devices are registered.')

        # Check the current status of WiFi
        wifi_enabled = self.check_wifi_status('enabled')

        # If WiFi is originally enabled, disable it
        if wifi_enabled:
            self.wifi_toggle(enable=False)
            assert not self.check_wifi_status('enabled'), 'Failed to disable Wi-Fi'

        # Enable WiFi and check status
        self.wifi_toggle(enable=True)
        assert self.check_wifi_status('enabled'), 'Failed to enable Wi-Fi'


if __name__ == '__main__':
    common_main()
