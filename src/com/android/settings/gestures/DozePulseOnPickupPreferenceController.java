package com.android.settings.gestures;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;

import com.android.settings.core.BasePreferenceController;

public class DozePulseOnPickupPreferenceController extends BasePreferenceController {

    public DozePulseOnPickupPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        AmbientDisplayConfiguration cfg = new AmbientDisplayConfiguration(mContext);
        return cfg.dozePickupSensorAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
