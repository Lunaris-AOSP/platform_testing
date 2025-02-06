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

class GPSLocationValidationTest(base_test.BaseTestClass):

    def setup_class(self):
        self.android_devices = self.register_controller(android_device)

    def toggle_location_settings(self, enable):
        """Enables or disables the location settings on the device.

        Args:
            enable (bool): True to enable location settings, False to disable.
        """
        state = 'true' if enable else 'false'
        cmd = f'cmd location set-location-enabled {state}'
        output = self.android_devices[0].adb.shell(cmd).decode('utf-8')
        if 'Error' in output:
            raise Exception(f'Unable to {state} location services: {output}')
        logging.info(f'Location services {state} command output: {output}')

    def check_location_status(self):
        """Checks if the user has enabled location on the device.

        Returns:
            bool: True if the location is enabled for the user, False otherwise.
        """
        try:
            status_output_bytes = self.android_devices[0].adb.shell('dumpsys location -a | grep "location \[u10\]"')
        except Exception as e:
            logging.warning("Received an AdbError: %s", e)
            # You can inspect e.stdout and e.stderr for more details if needed
            return False

        status_str = status_output_bytes.decode('utf-8')
        status_line = status_str.strip().split('\n')[-1]
        logging.info(status_line)
        location_enabled = "enabled" in status_line
        logging.info(f'User location status: {"enabled" if location_enabled else "disabled"}')
        return location_enabled

    def validate_last_location(self):
        """Validates if the longitude and latitude of the device is captured successfully.

        Returns:
            bool: True if valid GNSS location data is obtained, False otherwise.
        """
        location_output_bytes = self.android_devices[0].adb.shell('dumpsys location | grep "last location"')
        location_info = location_output_bytes.decode('utf-8').strip()
        logging.info(f'Last known location: {location_info}')
        return 'gps:' in location_info and 'long=' in location_info and 'lat=' in location_info

    def test_location_validation(self):
        """Runs the GPS and location validation test case."""
        # Ensure at least one device is available
        if not self.android_devices:
            raise AssertionError('No Android devices are registered.')

        # Toggle off location and verify it's off
        logging.info("Toggle off location and verify")
        self.toggle_location_settings(enable=False)
        assert not self.check_location_status(), 'Failed to disable location services'

        # Toggle on location and verify it's on
        logging.info("Toggle on location and verify")
        self.toggle_location_settings(enable=True)
        assert self.check_location_status(), 'Failed to enable location services'

        logging.info("verify last location")
        # Validate that the device captures a valid last known location
        assert self.validate_last_location(), 'Failed to capture a valid last known location'

if __name__ == '__main__':
    common_main()
