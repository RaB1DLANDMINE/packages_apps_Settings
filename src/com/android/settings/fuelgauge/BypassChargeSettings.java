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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings &gt; Battery &gt; Bypass charging.
 *
 * <p>Master switch ({@code Settings.System.bypass_charge_enabled}) plus a per-app allowlist
 * ({@code Settings.Secure.bypass_charge_apps}, ':'-joined). While the master is on, the platform
 * {@code BypassChargeController} holds the battery whenever any checked app is in the foreground.
 */
public class BypassChargeSettings extends SettingsPreferenceFragment {

    private static final String KEY_MASTER = "bypass_charge_enabled";  // Settings.System
    private static final String KEY_APPS = "bypass_charge_apps";       // Settings.Secure

    private final Set<String> mSelected = new LinkedHashSet<>();
    private PreferenceCategory mAppsCategory;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context context = getPreferenceManager().getContext();
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        loadSelected(context);

        final SwitchPreferenceCompat master = new SwitchPreferenceCompat(context);
        master.setKey(KEY_MASTER);
        master.setTitle(R.string.bypass_charge_master_title);
        master.setSummary(R.string.bypass_charge_master_summary);
        master.setPersistent(false);
        master.setChecked(isMasterEnabled(context));
        master.setOnPreferenceChangeListener((p, v) -> {
            Settings.System.putIntForUser(context.getContentResolver(), KEY_MASTER,
                    ((Boolean) v) ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        });
        screen.addPreference(master);

        mAppsCategory = new PreferenceCategory(context);
        mAppsCategory.setTitle(R.string.bypass_charge_apps_category);
        screen.addPreference(mAppsCategory);

        setPreferenceScreen(screen);
        populateApps(context);
    }

    private boolean isMasterEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), KEY_MASTER, 0,
                UserHandle.USER_CURRENT) == 1;
    }

    private void loadSelected(Context context) {
        mSelected.clear();
        final String list = Settings.Secure.getStringForUser(context.getContentResolver(),
                KEY_APPS, UserHandle.USER_CURRENT);
        if (!TextUtils.isEmpty(list)) {
            for (String p : list.split(":")) {
                if (!TextUtils.isEmpty(p)) {
                    mSelected.add(p);
                }
            }
        }
    }

    private void saveSelected(Context context) {
        Settings.Secure.putStringForUser(context.getContentResolver(), KEY_APPS,
                TextUtils.join(":", mSelected), UserHandle.USER_CURRENT);
    }

    private void populateApps(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Intent launcher =
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
        final Collator collator = Collator.getInstance();
        final ArrayList<Preference> prefs = new ArrayList<>();
        final Set<String> seen = new LinkedHashSet<>();

        for (ResolveInfo ri : apps) {
            final String pkg = ri.activityInfo != null ? ri.activityInfo.packageName : null;
            if (pkg == null || !seen.add(pkg)) {
                continue;
            }
            final SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
            pref.setKey(pkg);
            pref.setTitle(ri.loadLabel(pm));
            pref.setIcon(ri.loadIcon(pm));
            pref.setPersistent(false);
            pref.setChecked(mSelected.contains(pkg));
            pref.setOnPreferenceChangeListener((p, v) -> {
                if ((Boolean) v) {
                    mSelected.add(pkg);
                } else {
                    mSelected.remove(pkg);
                }
                saveSelected(context);
                return true;
            });
            prefs.add(pref);
        }

        Collections.sort(prefs, (a, b) ->
                collator.compare(a.getTitle().toString(), b.getTitle().toString()));
        int order = 0;
        for (Preference p : prefs) {
            p.setOrder(order++);
            mAppsCategory.addPreference(p);
        }
    }
}
