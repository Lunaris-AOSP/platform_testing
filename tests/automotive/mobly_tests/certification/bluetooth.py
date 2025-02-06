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
import time
from mobly import base_test
from mobly import test_runner
from mobly.controllers import android_device
from utilities.main_utils import common_main

class BluetoothValidationTest(base_test.BaseTestClass):

    def setup_class(self):
        self.android_devices = self.register_controller(android_device)

    def bluetooth_toggle(self, enable):
        """Enables or disables the Bluetooth on the device.

        Args:
            enable (bool): True to enable Bluetooth, False to disable.
        """
        state = 'enable' if enable else 'disable'
        cmd = f'cmd bluetooth_manager {state}'
        output = self.android_devices[0].adb.shell(cmd)
        logging.info(f'Bluetooth {state} command output: {output}')

    def check_bluetooth_status(self, expected_status):
        """Checks the current Bluetooth status against the expected status.

        Args:
            expected_status (int): The expected Bluetooth status, 1 for 'enabled' or 0 for 'disabled'.

        Returns:
            bool: True if the actual and expected statuses match, False otherwise.
        """
        status_output_bytes = self.android_devices[0].adb.shell('settings list global | grep ^bluetooth_on')
        # The output will be a string like 'bluetooth_on=1' or 'bluetooth_on=0'.
        status_output = status_output_bytes.decode('utf-8').strip()
        current_status = status_output.split('=')[-1]
        logging.info(f'Current Bluetooth status: {status_output}')
        return str(expected_status) == current_status

    def test_bluetooth_validation(self):
        # Ensure at least one device is available
        if not self.android_devices:
            raise AssertionError('No Android devices are registered.')

        # Check the current status of Bluetooth
        logging.info("check bluetooth")
        bluetooth_enabled = self.check_bluetooth_status(1)

        # If Bluetooth is originally enabled, disable it
        if bluetooth_enabled:
            logging.info("turn off bluetooth")
            self.bluetooth_toggle(enable=False)
            assert not self.check_bluetooth_status(1), 'Failed to disable Bluetooth'

        # Enable Bluetooth and check status
        logging.info("enable bluetooth")
        self.bluetooth_toggle(enable=True)
        time.sleep(2)
        assert self.check_bluetooth_status(1), 'Failed to enable Bluetooth'


if __name__ == '__main__':
    common_main()
