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

class VhalValidationTest(base_test.BaseTestClass):

    def setup_class(self):
        # Registers Android device controllers.
        self.android_devices = self.register_controller(android_device)

    def check_vhal_properties(self):
        """Checks if VHAL is listing expected properties.

        Returns:
            bool: True if the properties are listed as expected, False otherwise.
        """
        cmd = 'dumpsys android.hardware.automotive.vehicle.IVehicle/default --list'
        output_bytes = self.android_devices[0].adb.shell(cmd)
        output = output_bytes.decode('utf-8')
        #logging.info('Current VHAL properties: {}'.format(output))

        # Extract property ids from the output.
        properties_list = []
        for line in output.splitlines()[1:]:  # Skip the first line which contains "listing xx properties"
            if ':' in line:
                try:
                    property_id = int(line.split(':')[1].strip())
                    properties_list.append(property_id)
                except ValueError:
                    logging.error('Failed to parse property id from line: {}'.format(line))

        # Define expected properties, replace this with the actual list you're expecting.
        expected_properties = set([356582673,356517131]) # Example, properties 0 through 96.

        # Create a set of properties found in the output.
        found_properties = set(properties_list)

        # Check if any expected properties are missing.
        missing_properties = expected_properties - found_properties
        if missing_properties:
            logging.error('Missing VHAL properties: {}'.format(missing_properties))

        return not missing_properties

    def test_vhal_properties_validation(self):
        if not self.check_vhal_properties():
            raise AssertionError('VHAL properties are not as expected.')

if __name__ == '__main__':
    common_main()
