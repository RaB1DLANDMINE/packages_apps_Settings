/*
 * Copyright (C) 2025
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

import android.content.Context;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;

/**
 * Controls visibility of the Gestures entry that opens DoubleTapAmbientSettings.
 * Hides the entry when the device does not expose a doze double-tap sensor type.
 */
public class DoubleTapAmbientEntryPreferenceController extends BasePreferenceController {

    public DoubleTapAmbientEntryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        // If the config string is empty, feature is unsupported; hide the preference.
    final String sensorType = mContext.getResources().getString(
        com.android.internal.R.string.config_dozeDoubleTapSensorType);
    return TextUtils.isEmpty(sensorType) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
