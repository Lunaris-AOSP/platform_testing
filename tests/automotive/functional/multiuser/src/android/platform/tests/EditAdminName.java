/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.content.pm.UserInfo;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.helpers.SettingsConstants;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EditAdminName {

    public String INITIAL_USERNAME;
    public static final String EDIT_USERNAME = "editedName";
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    public EditAdminName() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void getUserNameFromSettings() {
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        INITIAL_USERNAME = initialUser.name;
    }

    @After
    public void goBackToHomeScreen() {
        mUsersHelper.get().editUserName(INITIAL_USERNAME);
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testEditAdminName() {
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        mUsersHelper.get().editUserName(EDIT_USERNAME);
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        assertTrue("Username is not changed", EDIT_USERNAME.equals(newUser.name));
    }
}
