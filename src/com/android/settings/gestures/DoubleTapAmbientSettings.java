/*
 * Copyright (C) 2019-2020 The Evolution X Project
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

package com.android.settings.gestures;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.infinity.support.preferences.SystemSettingSwitchPreference;

public class DoubleTapAmbientSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.double_tap_ambient_screen_settings);

        getActivity().getActionBar().setTitle(R.string.double_tap_title);

        // Disable the switch if the device doesn't provide a doze double-tap sensor type
    final String sensorType = getContext().getResources().getString(
                com.android.internal.R.string.config_dozeDoubleTapSensorType);
        final PreferenceScreen screen = getPreferenceScreen();
        final SystemSettingSwitchPreference pref = screen.findPreference("doze_trigger_doubletap");
        if (pref != null) {
            pref.setEnabled(TextUtils.isEmpty(sensorType));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INFINITY;
    }
}
