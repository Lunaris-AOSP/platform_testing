#  Copyright (C) 2023 The Android Open Source Project
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
import re
import time

from mobly.controllers import android_device
from utilities import constants


class CallUtilsError(Exception):
    """This exception may be expanded in the future to provide better error discoverability."""
    pass


class CallUtils:
    """Calling sequence utility for BT calling test using Spectatio UI APIs.

    This class provides functions that execute generic call sequences. Specific
    methods (e.g., verify_precall_state) are left to that implementation, and thus the
    utilities housed here are meant to describe generic sequences of actions.
    """

    def __init__(self, device):
        self.device = device

    def device_displays_connected(self):
        """Assumes the device bluetooth connection settings page is open"""
        logging.info('Checking whether device is connected.')
        self.device.mbs.deviceIsConnected()

    def open_app_grid(self):
        """Opens app Grid """
        logging.info("Opening app grid")
        self.device.mbs.openAppGrid()

    def dial_a_number(self, callee_number):
        """Dial phone number"""
        logging.info("Dial phone number <%s>", callee_number)
        self.device.mbs.dialANumber(callee_number)

    def end_call(self):
        """End the call. Throws an error if non call is currently ongoing."""
        logging.info("End the call")
        self.device.mbs.endCall()

    def execute_shell_on_device(self, device_target, shell_command):
        """Execute any shell command on any device"""
        logging.info(
            "Executing shell command: <%s> on device <%s>",
            shell_command,
            device_target.serial,
        )
        return device_target.adb.shell(shell_command)

    def get_dialing_number(self):
        """Get dialing phone number"""
        return self.device.mbs.getDialingNumber()

    def get_user_phone_number(self):
        """Get user phone number"""
        return self.device.mbs.getUserProfilePhoneNumber()

    def get_home_address_from_details(self):
        """Return the home address of the contact whose details are currently being displayed"""
        return self.device.mbs.getHomeAddress()

    def get_device_summary(self):
        """Assumes the device summary page is open."""
        return self.device.mbs.getDeviceSummary()

    def is_phone_profile_enabled(self):
        """Returns if phone profile is enabled on Bluetooth Summary Screen."""
        return self.device.mbs.isPhoneProfileEnabled()

    def import_contacts_from_vcf_file(self, device_target):
        """Importing contacts from VCF file"""
        logging.info("Importing contacts from VCF file to device Contacts")
        self.execute_shell_on_device(
            device_target,
            constants.IMPOST_CONTACTS_SHELL_COMAND,
        )

    def make_call(self):
        """Make call"""
        logging.info("Make a call")
        self.device.mbs.makeCall()

    def open_call_history(self):
        """Open call history"""
        logging.info("Open call history")
        self.device.mbs.openCallHistory()

    def open_contacts(self):
        """Open contacts"""
        logging.info("Opening contacts")
        self.device.mbs.openContacts()

    def open_favorites(self):
        """Open favorites"""
        logging.info("Opening favorites")
        self.device.mbs.openFavorites()

    def open_dialpad(self):
        """Open the dial pad from the dialer main screen"""
        logging.info("Opening the dialpad")
        self.device.mbs.openDialPad()

    def open_dialer_settings(self):
        logging.info("Opening the dialer settings")
        self.device.mbs.openDialerSettings()

    def open_phone_app(self):
        logging.info("Opening phone app")
        self.device.mbs.openPhoneApp()

    def open_first_contact_details(self):
        """Open the contact details page for the first contact visible in the contact list.
        Assumes we are on the contacts page."""
        logging.info("Getting details for first contact on the page")
        self.device.mbs.openFirstContactDetails()

    def open_bluetooth_settings(self):
        """Assumes we are on the home screen.
        Navigate to the Bluetooth setting page"""
        logging.info("Opening bluetooth settings (via the home screen)")
        self.device.mbs.openBluetoothSettings()

    def open_bluetooth_settings_form_status_bar(self):
        """Assumes we are on the home screen.
        Navigate to the Bluetooth setting page"""
        logging.info("Opening bluetooth settings (via the Status Bar)")
        self.device.mbs.openBluetoothSettingsFromStatusBar()

    def press_active_call_toggle(self):
        logging.info("Pressing the Active Call toggle")
        self.device.mbs.pressActiveCallToggle()

    def open_dialer_settings(self):
        logging.info('Opening Dialer settings')
        self.device.mbs.openDialerSettings()

    def press_bluetooth_toggle_on_device(self, device_name):
        logging.info('Attempting to press the bluetooth toggle on device: \'%s\'' % device_name)
        self.device.mbs.pressBluetoothToggleOnDevice(device_name)

    def press_media_toggle_on_device(self, device_name):
        logging.info('Attempting to press the media toggle on device: \'%s\'' % device_name)
        self.device.mbs.pressMediaToggleOnDevice(device_name)

    def press_contact_search_result(self, expected_first_name):
        logging.info('Attempting to press the contact result with name \'%s\'' % expected_first_name)
        self.device.mbs.pressContactResult(expected_first_name)

    def press_device_entry_on_list_of_paired_devices(self, device_name):
        logging.info('Attempting to press the device entry on device: ' + device_name)
        self.device.mbs.pressDeviceInBluetoothSettings(device_name)

    def press_home_screen_on_status_bar(self):
        """Presses the Home screen button on the status bar
        (to return the device to the home screen."""
        logging.info("Pressing home screen button")
        self.device.mbs.pressHomeScreen()

    def press_phone_toggle_on_device(self, device_name):
        logging.info('Attempting to press the phone toggle on device: \'%s\'' % device_name)
        self.device.mbs.pressPhoneToggleOnDevice(device_name)

    def press_dialer_button_on_phone_card(self):
        logging.info('Attempting to press the dialer button on the phone card')
        self.device.mbs.pressDialerButtonOnPhoneCard()

    def device_displays_connected(self):
        """Assumes the device bluetooth connection settings page is open"""
        logging.info('Checking whether device is connected.')
        self.device.mbs.deviceIsConnected()

    def press_forget(self):
        """Assumes the device bluetooth connection settings page is open"""
        logging.info('Attempting to press \'Forget\'')
        self.device.mbs.pressForget()

    def press_home(self):
        """Press the Home button to go back to the home page."""
        logging.info("Pressing HOME ")
        self.device.mbs.pressHome()

    def press_enter_on_device(self, device_target):
        logging.info("Pressing ENTER on device.")
        self.execute_shell_on_device(device_target, "input keyevent KEYCODE_ENTER")
        self.wait_with_log(constants.ONE_SEC)

    def push_vcf_contacts_to_device(self, device_target, path_to_contacts_file):
        """Pushing contacts file to device using adb command"""
        logging.info(
            "Pushing VCF contacts to device %s to destination <%s>",
            device_target.serial,
            constants.PHONE_CONTACTS_DESTINATION_PATH,
        )
        device_target.adb.push(
            [path_to_contacts_file, constants.PHONE_CONTACTS_DESTINATION_PATH],
            timeout=20,
        )

    def validate_three_preference_buttons(self, bluetooth_enabled):
        """Checks each of the three preference buttons (bluetooth, phone, audio).

        If bluetooth is enabled, all three buttons should be enabled, and the bluetooth
        button should be checked.

        If bluetooth is disabled, the bluetooth button should not be checked,
        and the phone and media buttons should be disabled.
        """
        logging.info("Checking the three Preference buttons on the listed device")
        expected_check_status = "checked" if bluetooth_enabled else "unchecked"
        if (self.device.mbs.isBluetoothPreferenceChecked() != bluetooth_enabled):
            logging.info("Bluetooth preference check status does not match expected status: "
                         + str(bluetooth_enabled))
            return False

        expected_status = "enabled" if bluetooth_enabled else "disabled"
        if (self.device.mbs.isPhonePreferenceEnabled()  != bluetooth_enabled):
            logging.info("Phone preference was does not match expected status: %s",
                         str(expected_status))
            return False

        if (self.device.mbs.isMediaPreferenceEnabled() != bluetooth_enabled):
            logging.info("Media preference does not match enabled status: " + str(expected_status))
            return False

        return True

    def upload_vcf_contacts_to_device(self, device_target, path_to_contacts_file):
        """Upload contacts do device"""
        self.import_contacts_from_vcf_file(device_target)
        device_target.mbs.pressDevice()

    def verify_contact_name(self, expected_contact):
        actual_dialed_contact = self.device.mbs.getContactName()
        logging.info(
            "Expected contact name being called: <%s>, Actual: <%s>",
            expected_contact,
            actual_dialed_contact,
        )
        if actual_dialed_contact != expected_contact:
            raise CallUtilsError(
                "Actual and Expected contacts on dial pad don't match."
            )

    def wait_with_log(self, wait_time):
        """Wait for specific time for debugging"""
        logging.info("Sleep for %s seconds", wait_time)
        time.sleep(wait_time)

    def open_details_page(self, contact_name):
        logging.info('open contacts details page')
        self.device.mbs.openDetailsPage(contact_name)

    def close_details_page(self):
        logging.info('close contacts details page')
        self.device.mbs.closeDetailsPage()

    def add_remove_favorite_contact(self):
        logging.info('add remove favorite contact')
        self.device.mbs.addRemoveFavoriteContact()

    def add_favorites_from_favorites_tab(self, contact_name):
        logging.info('add favorites from favorites tab')
        self.device.mbs.addFavoritesFromFavoritesTab(contact_name)

    def is_contact_in_favorites(self, contact_name, expected_result):
        logging.info('check if contact is in favorites')
        actual_result =self.device.mbs.isContactInFavorites(contact_name)
        logging.info(
            ' Add/Remove contacts expected : <%s>, Actual : <%s>',
            expected_result,
            actual_result,
        )
        if expected_result == 'True' and expected_result != actual_result:
            raise CallUtilsError('Contact not added to favorites')
        if expected_result == 'False' and expected_result != actual_result:
            raise CallUtilsError('Contact not removed from favorites')

    def open_bluetooth_media_app(self):
        logging.info('Open Bluetooth Audio app')
        self.device.mbs.openBluetoothMediaApp();
        self.wait_with_log(1)

    def open_bluetooth_sms_app(self):
        logging.info('Open Bluetooth SMS app')
        self.device.mbs.openSmsApp();
        self.wait_with_log(1)

    def click_phone_button(self):
        logging.info("Click phone button")
        self.device.mbs.clickPhoneButton()

    def verify_disabled_phone_profile(self):
        logging.info("Checks if phone profile is disabled")
        return self.device.mbs.isBluetoothPhoneButtonEnabled()

    def verify_bluetooth_hfp_error_displayed(self):
        logging.info("Checks if bluetooth hfp error")
        return self.device.mbs.isBluetoothHfpErrorDisplayed()

    def verify_dialer_recents_tab(self):
        logging.info("Checks if dialer recents tab is displayed")
        return self.device.mbs.verifyDialerRecentsTab()

    def verify_dialer_contacts_tab(self):
        logging.info("Checks if dialer contacts tab is displayed")
        return self.device.mbs.verifyDialerContactsTab()

    def verify_dialer_favorites_tab(self):
        logging.info("Checks if favorites tab is displayed")
        return self.device.mbs.verifyDialerFavoritesTab()

    def verify_dialer_dialpad_tab(self):
        logging.info("Checks if dialpad is displayed")
        return self.device.mbs.verifyDialerDialpadTab()

    def verify_dialer_search_lens(self):
        logging.info("Checks if Search lens is displayed")
        return self.device.mbs.verifyDialerSearchLens()


    def verify_dialer_settings(self):
        logging.info("Checks if dialer settings is displayed")
        return self.device.mbs.verifyDialerSettings()

    def open_sms_app(self):
        """Open sms app"""
        logging.info('Opening sms app')
        self.device.mbs.openSmsApp()

    def open_bluetooth_palette(self):
        logging.info("Open Bluetooth Palette")
        self.device.mbs.openBluetoothPalette()

    def click_bluetooth_button(self):
        logging.info("Click Bluetooth Button")
        self.device.mbs.clickBluetoothButton()

    def is_bluetooth_connected(self):
        logging.info("Bluetooth Connected Status")
        is_connected = self.device.mbs.isBluetoothConnected()
        return is_connected

    def is_bluetooth_audio_disconnected_label_visible(self):
        """Return is <Bluetooth Audio disconnected> label present """
        logging.info('Checking is <Bluetooth Audio disconnected> label present')
        actual_disconnected_label_status = self.device.mbs.isBluetoothAudioDisconnectedLabelVisible()
        logging.info('<Bluetooth Audio disconnected> label is present: %s',
                     actual_disconnected_label_status)
        return actual_disconnected_label_status

    def is_connect_to_bluetooth_label_visible_on_bluetooth_audio_page(self):
        """Return is <Connect to Bluetooth> label present """
        logging.info('Checking is <Connect to Bluetooth> label present')
        actual_status = self.device.mbs.isBluetoothAudioDisconnectedLabelVisible()
        logging.info('<Connect to Bluetooth> label is present: %s',actual_status)
        return actual_status

    def click_cancel_label_visible_on_bluetooth_audio_page(self):
        """Clicks on <Cancel> label present on bluetooth Audio page"""
        self.device.mbs.cancelBluetoothAudioConncetion()
        logging.info('Clicked on <Cancel> label present on bluetooth Audio page')

    def handle_bluetooth_audio_pop_up(self):
        """Close on BT audio popup if present on bluetooth Audio page"""
        logging.info('Adding wait to check the Bluetooth Audio popup')
        time.sleep(constants.DEFAULT_WAIT_TIME_FIVE_SECS)
        is_bluetooth_media_popup_present = self.is_connect_to_bluetooth_label_visible_on_bluetooth_audio_page()
        if is_bluetooth_media_popup_present:
          logging.info('BT Audio popup present, cancelling that.')
          self.click_cancel_label_visible_on_bluetooth_audio_page()

    def update_device_timezone(self, expected_timezone):
        logging.info('Update the device timezone to %s',
                     expected_timezone)
        self.device.mbs.setTimeZone(expected_timezone)
        actual_timezone = self.device.mbs.getTimeZone()
        logging.info('<actual timezone> : %s  and <expected_timezone> %s',
                     actual_timezone, expected_timezone)
        if expected_timezone not in actual_timezone:
            raise CallUtilsError(
                "Time Zone did not set properly."
            )

    def is_bluetooth_hfp_error_displayed(self):
        logging.info('Verify Bluetooth HFP error is displayed,'
                     'when bluetooth is disconnected')
        return self.device.mbs.isBluetoothHfpErrorDisplayed()

    def search_contacts(self):
        logging.info('Searching contacts using SEARCH_CONTACTS_WORKFLOW')
        self.device.mbs.searchContacts()

    def search_contacts_name(self, contact_name):
        logging.info('Searching <%s> in contacts', contact_name)
        self.device.mbs.searchContactsByName(contact_name)

    def sort_contacts_by_first_name(self):
        logging.info('Sorting contacts by first name')
        self.device.mbs.sortContactListByFirstName()

    def get_list_of_visible_contacts(self):
        logging.info('Getting list of visible contacts')
        actual_visible_contacts_list = self.device.mbs.getListOfAllContacts()
        logging.info(
            'Actual list of visible contacts: <%s>', actual_visible_contacts_list
        )
        return actual_visible_contacts_list

    def verify_ascending_sorting_order(self, actual_sorting_order):
        expected_sorting_order = sorted(actual_sorting_order)
        logging.info(
            'Expected sorting order: <%s>, Actual sorting order: <%s>',
            expected_sorting_order,
            actual_sorting_order,
        )
        if actual_sorting_order != expected_sorting_order:
            raise CallUtilsError("Actual and Expected sorting orders don't match.")

    def is_bluetooth_sms_disconnected_label_visible(self):
        """Return is <Bluetooth SMS disconnected> label present"""
        logging.info('Checking is <Bluetooth SMS disconnected> label present')
        actual_disconnected_label_status = self.device.mbs.isSmsBluetoothErrorDisplayed()
        logging.info('<Bluetooth SMS disconnected> label is present: %s',
                     actual_disconnected_label_status)
        return actual_disconnected_label_status

    def verify_dialing_number(self, expected_dialing_number):
        """Replace all non-digits characters to null"""
        actual_dialing_number = re.sub(r'\D', '', str(self.get_dialing_number()))
        logging.info('dialing number: %s',self.get_dialing_number())
        logging.info(
            'Expected dialing number: %s, Actual: %s',
            expected_dialing_number,
            actual_dialing_number,
        )
        if actual_dialing_number != expected_dialing_number:
            raise CallUtilsError(
                "Actual and Expected dialing numbers don't match.")

    def verify_user_phone_number(self, expected_dialing_number):
        """Replace all non-digits characters to null"""
        actual_dialing_number = re.sub(r'\D', '', str(self.get_user_phone_number()))
        logging.info(
            'Expected dialing number: %s, Actual: %s',
            expected_dialing_number,
            actual_dialing_number,
        )
        if actual_dialing_number != expected_dialing_number:
            raise CallUtilsError(
                "Actual and Expected dialing numbers don't match.")

    def is_ongoing_call_displayed_on_home(self, expected_result):
        logging.info('Open Home screen and verify the ongoing call')
        self.device.mbs.isOngoingCallDisplayedOnHome()
        actual_result = self.device.mbs.isOngoingCallDisplayedOnHome()
        logging.info(
            'Call Displayed on home expected : <%s>, Actual : <%s>',
            expected_result,
            actual_result,
        )
        if expected_result != actual_result:
            raise CallUtilsError('Ongoing call not displayed on home')

    def get_recent_call_history(self):
        actual_recent_call_from_history = self.device.mbs.getRecentCallHistory()
        logging.info(
            'The latest call from history: <%s>', actual_recent_call_from_history
        )
        return actual_recent_call_from_history

    def verify_last_dialed_number(self, expected_last_dialed_number):
        actual_last_dialed_number = self.get_recent_call_history()
        actual_last_dialed_number = ''.join(
            char for char in actual_last_dialed_number if char.isdigit()
        )
        logging.info(
            'Expected last called number: %s, Actual: %s',
            expected_last_dialed_number,
            actual_last_dialed_number,
        )
        if actual_last_dialed_number != expected_last_dialed_number:
            raise CallUtilsError(
                "Actual and Expected last dialed numbers don't match."
            )

    def open_phone_app_from_home(self):
        logging.info('Open Phone from Home Screen card.')
        self.device.mbs.openPhoneAppFromHome()

    def search_contact_by_name(self, search_contact_name):
        logging.info('Searching <%s> in contacts', search_contact_name)
        self.device.mbs.searchContactsByName(search_contact_name)

    def get_first_search_result(self):
        logging.info('Getting first search result')
        actual_first_search_result = self.device.mbs.getFirstSearchResult()
        logging.info('Actual first search result: <%s>', actual_first_search_result)
        return actual_first_search_result

    def verify_search_results_contain_target_search(self, expected_search_result):
        actual_search_result = self.get_first_search_result()
        logging.info(
            'Expected search result: <%s>, Actual search result: <%s>',
            expected_search_result,
            actual_search_result,
        )
        if expected_search_result not in actual_search_result:
            raise CallUtilsError('Actual search result does not contain Expected.')

    def verify_sms_app_unread_message(self):
        """Verify unread message badge on sms app"""
        logging.info('Verify Unread Message on SMS app')
        return self.device.mbs.isUnreadSmsDisplayed()

    def verify_sms_preview_text(self, text):
        """Verify sms preview text"""
        logging.info('Verify SMS Preview Text')
        return self.device.mbs.isSmsPreviewTextDisplayed(text)

    def tap_to_read_aloud(self):
        """Tap on Received Text message"""
        logging.info('Click on the text >>> tap to read aloud ')
        self.device.mbs.tapToReadAloud()

    def is_assistant_sms_transcription_plate_displayed(self,expected):
        """ Checks if assistant transcription plate has displayed """
        visibility_status = self.device.mbs.isAssistantSMSTranscriptionPlateDisplayed()
        if visibility_status == expected:
            logging.info("Assistant SMS Transcription plate has opened upon tapping the SMS.")
        return visibility_status

    def verify_sms_preview_timestamp(self):
        """Verify sms preview timestamp"""
        logging.info('Verify SMS Preview TimeStamp')
        return self.device.mbs.isSmsTimeStampDisplayed()

    def verify_sms_no_messages_displayed(self):
        """Verify no messages text is displayed"""
        logging.info('Verify No Messages Text displayed')
        return self.device.mbs.isNoMessagesDisplayed()

    def clear_sms_app(self, device_target):
        """Clearing the sms app"""
        logging.debug('clearing the sms app')
        # self.execute_shell_on_device(device_target, constants.ROOT)
        self.execute_shell_on_device(device_target, constants.DELETE_MESSAGING_DB)
        self.execute_shell_on_device(device_target, constants.CLEAR_MESSAGING_APP)

    def reboot_device(self, device_target):
        self.execute_shell_on_device(device_target, constants.REBOOT)

    def has_bluetooth_button(self):
        logging.info('Has Bluetooth Button ')
        return self.device.mbs.hasBluetoothButton()

    def has_bluetooth_palette_phone_button(self):
        logging.info('Has Phone Button')
        return self.device.mbs.hasBluetoothPalettePhoneButton()

    def has_bluetooth_palette_media_button(self):
        logging.info('Has Media Button')
        return self.device.mbs.hasBluetoothPaletteMediaButton()

    def verify_device_name(self):
        logging.info('Verify Device Name')
        return self.device.mbs.verifyDeviceName()

    def is_bluetooth_button_enabled(self):
        logging.info('Is Bluetooth Button Enabled')
        return self.device.mbs.isBluetoothButtonEnabled()

    def has_bluetooth_toggle_on_message_displayed(self):
        logging.info('Has Bluetooth Toggle On Message Displayed')
        return self.device.mbs.hasBluetoothPaletteToggleOnMessage()

    def has_bluetooth_toggle_off_message_displayed(self):
        logging.info('Has Bluetooth Toggle Off Message Displayed')
        return self.device.mbs.hasBluetoothPaletteToggleOffMessage()

    def turn_on_off_bluetooth_switch_on_palette(self, switchstate):
        self.device.mbs.turnOnOffBluetoothSwitchOnPalette(switchstate)
        logging.info('Expected bluetooth toggle switch status: <%s>',
                     switchstate)

    def is_bluetooth_toggle_switch_in_palette_on(self):
        logging.info('Verify whether bluetooth toggle button is enabled in Bluetooth palette')
        return self.device.mbs.isBluetoothToggleSwitchInPaletteOn()

    def is_active_call_enabled(self):
        logging.info("Verifying whether active call is enabled")
        return self.device.mbs.isActiveCallEnabled()

    def is_active_call_ongoing_full_screen(self):
        logging.info("Verify whether an ongoing call is currently showing in full-screen mode")
        return self.device.mbs.isOngoingCallInFullScreen()

    def is_bluetooth_phone_button_enabled(self):
        logging.info('Is Bluetooth Palette PhoneButton Enabled')
        return self.device.mbs.isBluetoothPhoneButtonEnabled()

    def is_bluetooth_media_button_enabled(self):
        logging.info('Is Bluetooth Palette Media Button Enabled')
        return self.device.mbs.isBluetoothMediaButtonEnabled()

    def get_dial_in_number(self):
        return self.device.mbs.getNumberInDialPad()

    def verify_dialed_number_on_dial_pad(self, expected_dialed_number):
        actual_dialed_number = self.get_dial_in_number()
        logging.info('Expected number on Dial Pad: <%s>, Actual: <%s>',
                     expected_dialed_number,
                     actual_dialed_number,)

        if actual_dialed_number != expected_dialed_number:
            raise CallUtilsError(
                "Actual and Expected dialing numbers on dial pad don't match.")

    def delete_dialed_number_on_dial_pad(self):
        logging.info('Deleting dialed number on Dial Pad')
        self.device.mbs.deleteDialedNumber()

    def end_call_using_adb_command(self, device_target):
        self.execute_shell_on_device(device_target, 'input keyevent KEYCODE_ENDCALL')
        self.execute_shell_on_device(device_target, 'input keyevent KEYCODE_POWER')

    def call_most_recent_call_history(self):
        logging.info('Calling most recent call in history')
        self.device.mbs.callMostRecentHistory()

    def change_audio_source_to_phone(self):
        logging.info('Changing audio source to PHONE')
        self.device.mbs.changeAudioSourceToPhone()

    def change_audio_source_to_car_speakers(self):
        logging.info('Changing audio source to CAR SPEAKERS')
        self.device.mbs.changeAudioSourceToCarSpeakers()

    def enable_driving_mode(self):
        logging.info('Enabling the drive mode')
        self.device.mbs.enableDrivingMode()

    def disable_driving_mode(self):
        logging.info('Disabling the drive mode')
        self.device.mbs.disableDrivingMode()

    def is_microphone_displayed_on_status_bar(self, expected_result):
        logging.info('Mute the call, verify microphone on status bar')
        actual_result = self.device.mbs.isMicChipPresentOnStatusBar()
        logging.info(
            'Microphone Chip on status bar expected : <%s>, Actual : <%s>',
            expected_result,
            actual_result,
        )
        if expected_result != actual_result:
            raise CallUtilsError(
                'MicroPhone Chip not in sync with call status'
            )

    def mute_call(self):
        logging.info('Muting call')
        self.device.mbs.muteCall()

    def unmute_call(self):
        logging.info('Unmuting call')
        self.device.mbs.unmuteCall()

    def click_on_bluetooth_palette_media_button(self):
        """Performs click operation on Bluetooth Palette media button"""
        self.device.mbs.clickOnBluetoothPaletteMediaButton()
        logging.info("Clicked on bluetooth palette media button")

    def open_notification_on_phone(self, device_target):
        """Open notifications on Phone"""
        logging.debug('Open notifications on Phone')
        self.execute_shell_on_device(device_target, constants.OPEN_NOTIFICATION)

    def press_phone_home_icon_using_adb_command(self, device_target):
        self.execute_shell_on_device(device_target, 'input keyevent KEYCODE_HOME')

    def get_bt_profile_status_using_adb_command(self, device_target, profile):
        try:
           bt_profile_status = self.execute_shell_on_device(device_target, profile).decode('utf8')
           logging.debug(bt_profile_status)
           return bt_profile_status
        except adb.AdbError:
           logging.info("Adb returned null")

    def get_bt_connection_status_using_adb_command(self, device_target):
        try:
            bt_connection_status = self.execute_shell_on_device(device_target, constants.BLUETOOTH_CONNECTION_STATE).decode('utf8')
            logging.debug(bt_connection_status)
            return bt_connection_status
        except adb.AdbError:
            logging.info("Adb returned null")
