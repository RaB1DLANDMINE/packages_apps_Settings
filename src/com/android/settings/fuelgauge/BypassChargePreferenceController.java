/*
 * Copyright (C) 2026 Project Infinity X
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
package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.core.BasePreferenceController;

/**
 * Gates the Battery &gt; "Bypass charging" entry to devices whose charging-control backend supports
 * it (same prop the old GameSpace toggle used). Opens {@link BypassChargeSettings}.
 */
public class BypassChargePreferenceController extends BasePreferenceController {

    public BypassChargePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return SystemProperties.getBoolean("persist.sys.battery_bypass_supported", false)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
