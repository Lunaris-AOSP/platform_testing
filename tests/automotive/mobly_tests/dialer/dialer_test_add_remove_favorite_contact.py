"""
  Copyright (C) 2023 The Android Open Source Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.


  Test Steps:
        1) Precall state check on Seahawk and phone devices.
        2) Upload contacts.vcf to device.
        3) Add the contact as favorite
        4) Assert the contact is added to favorites
        5) Remove the contact form favorites
        6) Verify contact is removed from the favorites
"""

from bluetooth_test import bluetooth_base_test

from utilities import constants
from utilities.main_utils import common_main
from mobly import asserts

class AddRemoveFavoriteContact(bluetooth_base_test.BluetoothBaseTest):
  """Enable and Disable Bluetooth from Bluetooth Palette."""

  def setup_test(self):
    """Setup steps before any test is executed."""

    # Upload contacts to phone device
    file_path = constants.PATH_TO_CONTACTS_VCF_FILE
    self.call_utils.upload_vcf_contacts_to_device(self.target, file_path)

    self.call_utils.wait_with_log(5)
    # Pair caller phone with automotive device
    self.bt_utils.pair_primary_to_secondary()
    super().enable_recording()

  def test_add_remove_favorite_contact(self):
    """Tests add remove favorite contact."""
    contact_name = "Adam Allen"
    self.call_utils.open_phone_app()

    # Adding the contacts to favorites from the favorites tab and verifying it
    self.call_utils.add_favorites_from_favorites_tab(
        contact_name)
    self.call_utils.open_contacts()
    self.call_utils.open_favorites()
    asserts.assert_true(self.discoverer.mbs.hasUIElementWithText(contact_name),
                        'Favorite contact should be displayed on Favorites Tab')
    self.discoverer.mbs.clickUIElementWithText(contact_name)

    self.call_utils.wait_with_log(2)
    self.call_utils.is_ongoing_call_displayed_on_home(True)
    self.call_utils.open_phone_app_from_home()
    asserts.assert_true(self.discoverer.mbs.hasUIElementWithText(contact_name),
                        'Favorite contact should be displayed on homescreen during call')

    self.call_utils.end_call()
    self.call_utils.is_contact_in_favorites(
        contact_name, True)
    self.call_utils.wait_with_log(10)

    # Removing the contacts from favorites and verifying it
    self.call_utils.open_details_page(contact_name)
    self.call_utils.add_remove_favorite_contact()
    self.call_utils.close_details_page()
    self.call_utils.is_contact_in_favorites(
        contact_name, False)

  def teardown_test(self):
      # End call if test failed
    self.call_utils.end_call_using_adb_command(self.target)
    self.call_utils.wait_with_log(5)
    self.call_utils.press_home()
    super().teardown_test()

if __name__ == '__main__':
    common_main()