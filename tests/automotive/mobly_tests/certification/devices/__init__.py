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

from .seahawk import DeviceSeahawk

class DeviceFactory:
    @staticmethod
    def get_device(device_name: str):
        """Factory method to get the corresponding DeviceType based on device name."""
        if device_name == 'seahawk':
            return DeviceSeahawk

        raise ValueError(f"No such device with name: {device_name}")
