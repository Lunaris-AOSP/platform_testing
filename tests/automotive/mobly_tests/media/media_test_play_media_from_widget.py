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

from bluetooth_test import bluetooth_base_test
from mobly import asserts
from utilities.media_utils import MediaUtils
from utilities.common_utils import CommonUtils
from utilities.main_utils import common_main


class IsAbleToPlayMediaFromWidgetTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
        super().setup_class()
        self.media_utils = MediaUtils(self.target, self.discoverer)
        self.common_utils = CommonUtils(self.target, self.discoverer)

    def setup_test(self):
        self.common_utils.grant_local_mac_address_permission()
        self.common_utils.enable_wifi_on_phone_device()
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()
        self.media_utils.enable_bt_media_debugging_logs()

    def test_media_is_song_playing(self):
        """Tests validating play/pause media on HU"""
        self.media_utils.open_media_app_on_hu()
        self.call_utils.handle_bluetooth_audio_pop_up()
        self.media_utils.open_youtube_music_app()
        logging.info("Getting song title from phone device: %s", self.media_utils.get_song_title_from_phone())
        self.media_utils.press_pause_song_button()
        self.call_utils.wait_with_log(3)
        asserts.assert_false(self.media_utils.is_song_playing_on_hu(),
                             'Media player should be on PAUSE mode')
        self.media_utils.play_media_on_hu()
        self.call_utils.wait_with_log(3)
        asserts.assert_true(self.media_utils.is_song_playing_on_hu(),
                            'Media player should be on PLAY mode')

    def teardown_test(self):
        self.media_utils.get_bt_dumpsys_metadata()
        #   Close YouTube Music app
        self.media_utils.close_youtube_music_app()
        self.call_utils.press_home()
        super().teardown_test()


if __name__ == '__main__':
    common_main()
