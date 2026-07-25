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

package com.android.settings.core;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.theme.R;

import java.util.ArrayList;
import java.util.List;

public class RoundCornerPreferenceAdapter extends PreferenceGroupAdapter {

    private static final int ROUND_CORNER_CENTER = 1;
    private static final int ROUND_CORNER_TOP = 1 << 1;
    private static final int ROUND_CORNER_BOTTOM = 1 << 2;

    private final PreferenceGroup mPreferenceGroup;

    private List<Integer> mRoundCornerMappingList;

    private final Handler mHandler;

    private final Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            updatePreferences();
        }
    };

    public RoundCornerPreferenceAdapter(@NonNull PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
        mPreferenceGroup = preferenceGroup;
        mHandler = new Handler(Looper.getMainLooper());
        updatePreferences();
    }

    @Override
    public void onPreferenceHierarchyChange(@NonNull Preference preference) {
        super.onPreferenceHierarchyChange(preference);
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    protected @DrawableRes int getRoundCornerDrawableRes(int position, boolean isSelected) {
        int CornerType = mRoundCornerMappingList.get(position);

        if ((CornerType & ROUND_CORNER_CENTER) == 0) {
            return 0;
        }

        if (((CornerType & ROUND_CORNER_TOP) != 0) && ((CornerType & ROUND_CORNER_BOTTOM) == 0)) {
            // the first
            return isSelected
                    ? SettingsThemeHelper.isExpressiveTheme(mPreferenceGroup.getContext())
                            ? R.drawable.settingslib_round_background_top_selected
                            : com.android.settings.R.drawable.round_background_top_selected
                    : R.drawable.settingslib_round_background_top;
        } else if (((CornerType & ROUND_CORNER_BOTTOM) != 0)
                && ((CornerType & ROUND_CORNER_TOP) == 0)) {
            // the last
            return isSelected
                    ? SettingsThemeHelper.isExpressiveTheme(mPreferenceGroup.getContext())
                            ? R.drawable.settingslib_round_background_bottom_selected
                            : com.android.settings.R.drawable.round_background_bottom_selected
                    : R.drawable.settingslib_round_background_bottom;
        } else if (((CornerType & ROUND_CORNER_TOP) != 0)
                && ((CornerType & ROUND_CORNER_BOTTOM) != 0)) {
            // the only one preference
            return isSelected
                    ? SettingsThemeHelper.isExpressiveTheme(mPreferenceGroup.getContext())
                            ? R.drawable.settingslib_round_background_selected
                            : com.android.settings.R.drawable.round_background_selected
                    : R.drawable.settingslib_round_background;
        } else {
            // in the center
            return isSelected
                    ? SettingsThemeHelper.isExpressiveTheme(mPreferenceGroup.getContext())
                            ? R.drawable.settingslib_round_background_center_selected
                            : com.android.settings.R.drawable.round_background_center_selected
                    : R.drawable.settingslib_round_background_center;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    private void updatePreferences() {
        mRoundCornerMappingList = new ArrayList<>();
        mappingPreferenceGroup(mRoundCornerMappingList, mPreferenceGroup);
    }

    private void mappingPreferenceGroup(List<Integer> visibleList, PreferenceGroup group) {
        int groupSize = group.getPreferenceCount();
        int firstVisible = 0;
        int lastVisible = 0;
        for (int i = 0; i < groupSize; i++) {
            Preference pref = group.getPreference(i);
            if (!pref.isVisible()) {
                continue;
            }

            //the first visible preference.
            Preference firstVisiblePref = group.getPreference(firstVisible);
            if (!firstVisiblePref.isVisible()) {
                firstVisible = i;
            }

            int value = 0;
            if (group instanceof PreferenceCategory) {
                if (pref instanceof PreferenceCategory) {
                    visibleList.add(value);
                    mappingPreferenceGroup(visibleList, (PreferenceCategory) pref);
                } else {
                    if (i == firstVisible) {
                        value |= ROUND_CORNER_TOP;
                    }

                    value |= ROUND_CORNER_BOTTOM;
                    if (i > lastVisible) {
                        // the last
                        int lastIndex = visibleList.size() - 1;
                        int newValue = visibleList.get(lastIndex) & ~ROUND_CORNER_BOTTOM;
                        visibleList.set(lastIndex, newValue);
                        lastVisible = i;
                    }

                    value |= ROUND_CORNER_CENTER;
                    visibleList.add(value);
                }
            } else {
                visibleList.add(value);
                if (pref instanceof PreferenceCategory) {
                    mappingPreferenceGroup(visibleList, (PreferenceCategory) pref);
                }
            }
        }
    }

    /** handle roundCorner background */
    private void updateBackground(PreferenceViewHolder holder, int position) {
        @DrawableRes int backgroundRes = getRoundCornerDrawableRes(position, false /* isSelected*/);

        View v = holder.itemView;
        v.setBackgroundResource(backgroundRes);
        applyGlassIfEnabled(v);
    }

    // Glass UI (Settings): when glass_ui_settings is on, make the card background translucent so
    // the frosted/animated backdrop shows through. No-op (normal opaque card) when off. Safe on
    // recycling: setBackgroundResource() is always called first, so the alpha never accumulates.
    private static final int GLASS_SETTINGS_CARD_ALPHA = 0xA6; // ~65%, lets the colour scroll show
    // Sheen stroke on glass cards. Bright (~80%) so it stays visible after the card's alpha
    // dimming (~65%) knocks it down to a clean ~52% white rim, matching the QS glass tiles.
    private static final int GLASS_SETTINGS_SHEEN_COLOR = 0xCCFFFFFF;

    // Unwrap Ripple/Layer/Inset/Scale drawables to the underlying GradientDrawable, so the sheen
    // stroke can be applied to the shape that actually defines the card's rounded corners.
    private static android.graphics.drawable.GradientDrawable findGradientDrawable(Drawable d) {
        if (d instanceof android.graphics.drawable.GradientDrawable) {
            return (android.graphics.drawable.GradientDrawable) d;
        }
        if (d instanceof android.graphics.drawable.LayerDrawable) {
            final android.graphics.drawable.LayerDrawable ld =
                    (android.graphics.drawable.LayerDrawable) d;
            for (int i = 0; i < ld.getNumberOfLayers(); i++) {
                final android.graphics.drawable.GradientDrawable found =
                        findGradientDrawable(ld.getDrawable(i));
                if (found != null) {
                    return found;
                }
            }
        }
        if (d instanceof android.graphics.drawable.DrawableWrapper) {
            return findGradientDrawable(
                    ((android.graphics.drawable.DrawableWrapper) d).getDrawable());
        }
        return null;
    }

    protected void applyGlassIfEnabled(View v) {
        if (v == null) {
            return;
        }
        final boolean on = Settings.System.getIntForUser(v.getContext().getContentResolver(),
                "glass_ui_settings", 0, UserHandle.USER_CURRENT) != 0;
        if (!on) {
            return;
        }
        final Drawable bg = v.getBackground();
        if (bg != null) {
            final Drawable mutated = bg.mutate();
            mutated.setAlpha(GLASS_SETTINGS_CARD_ALPHA);
            // Glass UI: the card background is often wrapped (Ripple/Layer/Inset), so unwrap to
            // the real GradientDrawable and stroke IT - that way the sheen exactly follows each
            // card's own corners (including the different radii of grouped top/middle/bottom
            // cards). The stroke is brightened to survive the card's alpha dimming.
            final float density = v.getResources().getDisplayMetrics().density;
            final android.graphics.drawable.GradientDrawable gd = findGradientDrawable(mutated);
            if (gd != null) {
                gd.setStroke(Math.max(1, Math.round(1.2f * density)), GLASS_SETTINGS_SHEEN_COLOR);
            }
            v.setForeground(null);
            v.setBackground(mutated);
        }
    }
}
