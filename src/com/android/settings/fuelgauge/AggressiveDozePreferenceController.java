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

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Battery &gt; "Aggressive deep sleep" master switch. Defaults on; see {@link AggressiveDoze}.
 */
public class AggressiveDozePreferenceController extends TogglePreferenceController {

    public AggressiveDozePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return AggressiveDoze.isEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        AggressiveDoze.setEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
