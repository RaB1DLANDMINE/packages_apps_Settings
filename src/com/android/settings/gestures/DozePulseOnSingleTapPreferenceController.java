package com.android.settings.gestures;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;

import com.android.settings.core.BasePreferenceController;

public class DozePulseOnSingleTapPreferenceController extends BasePreferenceController {

    public DozePulseOnSingleTapPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        // Visible only if single-tap (tap sensor) is supported
        AmbientDisplayConfiguration cfg = new AmbientDisplayConfiguration(mContext);
        return cfg.tapSensorAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
