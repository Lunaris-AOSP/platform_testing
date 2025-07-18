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

"""Android platform test annotation module."""

from .src.py.ApiTest import ApiTest
from .src.py.CddTest import CddTest
from .src.py.DesktopTest import DesktopTest
from .src.py.GmsTest import GmsTest
from .src.py.NonApiTest import NonApiTest
from .src.py.ReasonType import ReasonType
from .src.py.VsrTest import VsrTest
