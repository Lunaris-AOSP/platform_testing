/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "vkms_tester.h"
#include <log/log.h>
#include <stdlib.h>
#include <unistd.h>

/*
 * A simple binary that turns off vkms and deletes the vkms directory.
 *
 * Usage:
 *   ./teardown_vkms
 *
 * The binary will turn off the vkms (virtual kernel mode setting) driver
 * and delete the vkms directory.
 */

int main() {
  hcct::VkmsTester::ForceDeleteVkmsDir();
  return 0;
}