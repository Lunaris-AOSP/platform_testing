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

import subprocess
import os
import logging

from .types import DeviceType

class DeviceSeahawk(DeviceType):

  @staticmethod
  def flash(serial, imagedir, wipe, hwVariant, b, diag, preserveKeys):
    scriptName = 'fastboot_complete_seahawk.py'
    flashScript = imagedir / scriptName
    if not flashScript.exists():
      print('Flashing script was not downloaded, therefore aborting. Try again to run '
          + 'aae image pull')
      sys.exit(-1)

    args = ['python3', str(flashScript)]
    os.environ['ANDROID_PRODUCT_OUT'] = str(imagedir)
    os.environ['ANDROID_SERIAL'] = serial
    args.append('2s')
    if not wipe:
      args.append('--no-wipe')
    if not b:
      args.append('--no-b')
    if diag:
      args.append('--enable-diag')
    if preserveKeys:
      args.append('--preserve-keys')
    logging.info(args)
    command = " ".join(str(x) for x in args)
    try:
      # Start the subprocess with Popen and stream the output
      with subprocess.Popen(command, shell=True, executable='/bin/bash', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True) as proc:
        logging.info('Flashing the device...')
        # Stream the output line by line
        for line in iter(proc.stdout.readline, ''):
          logging.info(line.rstrip())  # Log the output lines in real-time
        proc.wait()  # Wait for the subprocess to finish
        if proc.returncode != 0:
          raise subprocess.CalledProcessError(proc.returncode, command)
    except subprocess.CalledProcessError as e:
      logging.error('Flash command failed with return code %s', e.returncode)
      raise
