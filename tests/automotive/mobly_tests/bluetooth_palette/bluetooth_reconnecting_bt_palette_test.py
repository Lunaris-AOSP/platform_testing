#  Copyright (C) 2024 The Android Open Source Project
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

"""
Test Steps:
(0. Flash device)
1. Launch Home - Action Bar - Tap Bluetooth button - Bluetooth Palette with be displayed
2. Toggle Off On 'Bluetooth' button in Bluetooth palette
3. Verification:-Bluetooth Device disconnected displayed in the Bluetooth Palette
-Status in Bluetooth palette is 'To see your devices, turn on Bluetooth'
4. Toggle On Bluetooth Button in Bluetooth palette
5. Verification:- Bluetooth Device reconnected, and showing as Connected with Bluetooth Profiles button showing as enabled
6. Toggle Off/On 'Use Bluetooth' in Bluetooth Palette Multiple times

"""

from bluetooth_test import bluetooth_base_test
from mobly import asserts

from utilities import constants
from utilities.main_utils import common_main


class BluetoothReconnectingBTPalette(bluetooth_base_test.BluetoothBaseTest):
    """Enable and Disable Bluetooth from Bluetooth Palette."""

    def setup_test(self):
        """Setup steps before any test is executed."""
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()
        self.call_utils.press_home()

    def test_enable_disable_bluetooth_toggle_on_palette(self):
        """Tests enable and disable functionality of bluetooth."""
        self.call_utils.open_bluetooth_palette()
        self.call_utils.wait_with_log(10)
        """Verify BT profile button and status on the Bluetooth Palette when Bluetooth is ON"""
        asserts.assert_true(self.call_utils.is_bluetooth_toggle_switch_in_palette_on(), "Bluetooth Toggle Switch is on in Bluetooth palette")
        asserts.assert_true(self.call_utils.is_bluetooth_button_enabled(), "Bluetooth profile button is Enabled")
        asserts.assert_true(self.call_utils.is_bluetooth_connected(), "Bluetooth Connected status is displayed")
        self.call_utils.turn_on_off_bluetooth_switch_on_palette(False)
        """Verify BT profile button and status on the Bluetooth Palette when Bluetooth is OFF"""
        self.call_utils.wait_with_log(5)
        asserts.assert_false(self.call_utils.is_bluetooth_toggle_switch_in_palette_on(), "Bluetooth Toggle Switch is on in Bluetooth palette")
        asserts.assert_true(self.call_utils.has_bluetooth_toggle_off_message_displayed(), "Bluetooth OFF Message is displayed")
        """Turning Bluetooth switch ON to repeat the verification """
        self.call_utils.turn_on_off_bluetooth_switch_on_palette(True)
        self.call_utils.wait_with_log(5)
        """Verify BT profile button and status on the Bluetooth Palette when Bluetooth is ON"""
        asserts.assert_true(self.call_utils.is_bluetooth_toggle_switch_in_palette_on(), "Bluetooth Toggle Switch is on in Bluetooth palette")
        asserts.assert_true(self.call_utils.is_bluetooth_button_enabled(), "Bluetooth profile button is Enabled")
        asserts.assert_true(self.call_utils.is_bluetooth_connected(), "Bluetooth Connected status is displayed")
        self.call_utils.turn_on_off_bluetooth_switch_on_palette(False)
        """Verify BT profile button and status on the Bluetooth Palette when Bluetooth is OFF"""
        self.call_utils.wait_with_log(5)
        asserts.assert_false(self.call_utils.is_bluetooth_toggle_switch_in_palette_on(), "Bluetooth Toggle Switch is on in Bluetooth palette")
        asserts.assert_true(self.call_utils.has_bluetooth_toggle_off_message_displayed(), "Bluetooth OFF Message is displayed")
        """Turning Bluetooth switch ON """
        self.call_utils.turn_on_off_bluetooth_switch_on_palette(True)
        self.call_utils.wait_with_log(5)

if __name__ == '__main__':
    common_main()
