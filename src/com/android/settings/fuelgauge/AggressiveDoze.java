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
import android.provider.Settings;

/**
 * Shared state + logic for the Battery &gt; "Aggressive deep sleep" switch.
 *
 * <p>The switch flips a single Global flag ({@link #KEY_AGGRESSIVE_DOZE}); this helper translates
 * that flag into the platform's {@link Settings.Global#DEVICE_IDLE_CONSTANTS} override. When on, the
 * device enters deep Doze a few minutes after screen-off and keeps maintenance windows sparse; when
 * off, the override is cleared and Doze reverts to stock defaults.
 *
 * <p>High-priority FCM messages bypass Doze regardless of these constants, so real-time push
 * (messaging, calls, 2FA) is unaffected; only low-priority background pings batch to the next
 * maintenance window.
 */
public final class AggressiveDoze {

    private AggressiveDoze() {}

    /** Global int flag backing the switch. 1 = on. */
    public static final String KEY_AGGRESSIVE_DOZE = "aggressive_doze";

    /** Default ON, so a clean flash ships with the tuned idle profile already applied. */
    public static final int DEFAULT_ENABLED = 1;

    /**
     * Aggressive {@code device_idle_constants}: reach deep IDLE ~3 min after screen-off, stretch
     * maintenance windows (idle_to 1h growing x2 up to 6h), and hold alarms to >=1h in idle. A null
     * value (see {@link #applyState}) lets the platform fall back to its stock defaults.
     */
    static final String AGGRESSIVE_IDLE_CONSTANTS =
            "inactive_to=60000,sensing_to=15000,locating_to=15000,motion_inactive_to=30000,"
            + "idle_after_inactive_to=60000,idle_pending_to=60000,max_idle_pending_to=120000,"
            + "idle_pending_factor=2.0,quick_doze_delay_to=60000,idle_to=3600000,"
            + "max_idle_to=21600000,idle_factor=2.0,min_time_to_alarm=3600000,"
            + "wait_for_unlock=false,light_after_inactive_to=120000,light_idle_to=300000,"
            + "light_max_idle_to=1800000,light_idle_factor=2.0,"
            + "light_idle_maintenance_min_budget=60000,light_idle_maintenance_max_budget=300000";

    /** Whether the switch is on (defaults to {@link #DEFAULT_ENABLED} when never set). */
    public static boolean isEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                KEY_AGGRESSIVE_DOZE, DEFAULT_ENABLED) == 1;
    }

    /** Persist the switch state and immediately apply the matching idle profile. */
    public static void setEnabled(Context context, boolean enabled) {
        Settings.Global.putInt(context.getContentResolver(),
                KEY_AGGRESSIVE_DOZE, enabled ? 1 : 0);
        applyState(context, enabled);
    }

    /** Push the aggressive constants (on) or clear the override entirely (off). */
    public static void applyState(Context context, boolean enabled) {
        // null removes the override so DeviceIdleController rebuilds from its built-in defaults.
        Settings.Global.putString(context.getContentResolver(),
                Settings.Global.DEVICE_IDLE_CONSTANTS,
                enabled ? AGGRESSIVE_IDLE_CONSTANTS : null);
    }

    /** Re-apply whatever the current flag says (used on boot). */
    public static void applyCurrent(Context context) {
        applyState(context, isEnabled(context));
    }
}
