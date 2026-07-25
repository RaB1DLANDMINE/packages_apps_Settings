/*
 * Copyright (C) 2024-25 Project Infinity X
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

package com.android.settings.utils;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class IconTinterUtils {
    private static final String ICON_STYLE = "icon_style";
    private static final String ICON_RANDOM_COLORS = "icon_random_colors";
    private static final String ICON_CORNER_STYLE = "icon_corner_style";
    
    private static final int ICON_STYLE_SOLID_BG_WHITE_ICON = 0;
    private static final int ICON_STYLE_GRADIENT_BG_WHITE_ICON = 1;
    private static final int ICON_STYLE_ALPHA_BG_COLOR_ICON = 2;
    private static final int ICON_STYLE_ACCENT_OUTLINE_ACCENT_ICON = 3;
    private static final int ICON_STYLE_SOLID_OUTLINE_SOLID_ICON = 4;
    private static final int ICON_STYLE_COLOR_ICON_NO_BG = 5;
    private static final int ICON_STYLE_DISABLED = 6;
    
    private static final int CORNER_STYLE_ROUND = 0;
    private static final int CORNER_STYLE_SQUARISH = 1;
    
    private static final int[] PASTEL_COLOR_RES_IDS = {
        R.color.pastel_blue,
        R.color.pastel_pink,
        R.color.pastel_purple,
        R.color.pastel_yellow,
        R.color.pastel_green,
        R.color.pastel_orange,
        R.color.pastel_lime,
        R.color.pastel_rose,
        R.color.pastel_teal,
        R.color.pastel_coral
    };
    
    private static final int ROUND_CORNER_RADIUS_DP = 99;
    private static final int SQUARISH_CORNER_RADIUS_DP = 15;
    private static final int BG_PADDING_DP = 8;
    private static final int OUTLINE_WIDTH_DP = 2;
    private static final float BG_ALPHA = 0.4f;
    private static final float ICON_SATURATION_BOOST = 0.3f;
    private static final float SOLID_BG_SATURATION_BOOST = 0.5f;
    private static final float GRADIENT_PASTEL_FACTOR = 0.5f;
    private static final float GRADIENT_SATURATION_FACTOR = 0.5f;
    
    private static final Map<String, Integer> colorCache = new HashMap<>();
    private static final Random random = new Random();

    public static void tintIcons(PreferenceScreen screen, Context context) {
        if (screen == null || context == null) return;
        
    int iconStyle = Settings.System.getIntForUser(
                context.getContentResolver(),
        ICON_STYLE,
        ICON_STYLE_ACCENT_OUTLINE_ACCENT_ICON,
                UserHandle.USER_CURRENT);
                
        boolean randomColors = Settings.System.getIntForUser(
                context.getContentResolver(),
                ICON_RANDOM_COLORS,
                0,
                UserHandle.USER_CURRENT) == 1;
        
        int cornerStyle = Settings.System.getIntForUser(
                context.getContentResolver(),
                ICON_CORNER_STYLE,
                CORNER_STYLE_ROUND,
                UserHandle.USER_CURRENT);
        
        Resources res = context.getResources();
        
        if (randomColors) {
            colorCache.clear();
        }
        
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);
            tintPreferenceIcon(preference, context, res, iconStyle, randomColors, cornerStyle);
            
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup group = (PreferenceGroup) preference;
                for (int j = 0; j < group.getPreferenceCount(); j++) {
                    tintPreferenceIcon(group.getPreference(j), context, res, iconStyle, randomColors, cornerStyle);
                }
            }
        }
    }

    /**
     * Apply the configured Settings icon style to a single Preference's current icon.
     * Safe no-op if preference or icon is null.
     */
    public static void tintSinglePreferenceIcon(Preference preference, Context context) {
    if (preference == null || context == null) return;

    final Drawable icon = preference.getIcon();
    if (icon == null) return;

    final int iconStyle = Settings.System.getIntForUser(
        context.getContentResolver(),
        ICON_STYLE,
        ICON_STYLE_SOLID_BG_WHITE_ICON,
        UserHandle.USER_CURRENT);

    final boolean randomColors = Settings.System.getIntForUser(
        context.getContentResolver(),
        ICON_RANDOM_COLORS,
        0,
        UserHandle.USER_CURRENT) == 1;

    final int cornerStyle = Settings.System.getIntForUser(
        context.getContentResolver(),
        ICON_CORNER_STYLE,
        CORNER_STYLE_ROUND,
        UserHandle.USER_CURRENT);

    final Resources res = context.getResources();
    tintPreferenceIcon(preference, context, res, iconStyle, randomColors, cornerStyle);
    }

    private static void tintPreferenceIcon(Preference preference, Context context, 
            Resources res, int iconStyle, boolean randomColors, int cornerStyle) {
        Drawable icon = preference.getIcon();
        if (icon == null) return;
        
        icon = icon.mutate();
        
        int baseColor;
        if (randomColors) {
            baseColor = getRandomPastelColor(res);
        } else {
            baseColor = getCachedColorForPreference(preference.getKey(), res);
        }
        
        int cornerRadiusDp = cornerStyle == CORNER_STYLE_ROUND ? 
                ROUND_CORNER_RADIUS_DP : SQUARISH_CORNER_RADIUS_DP;
        
        switch (iconStyle) {
            case ICON_STYLE_SOLID_BG_WHITE_ICON:
                int saturatedBgColor = increaseSaturation(baseColor, SOLID_BG_SATURATION_BOOST);
                applyIconWithBackground(
                    preference, icon, saturatedBgColor, 
                    false,
                    Color.WHITE, 
                    cornerRadiusDp, 
                    context
                );
                break;
                
            case ICON_STYLE_ALPHA_BG_COLOR_ICON:
                applyIconWithBackground(
                    preference, icon, baseColor, 
                    true,
                    increaseSaturation(baseColor, ICON_SATURATION_BOOST), 
                    cornerRadiusDp, 
                    context
                );
                break;
                
            case ICON_STYLE_GRADIENT_BG_WHITE_ICON:
                applyIconWithGradientBackground(
                    preference, icon, baseColor,
                    cornerRadiusDp,
                    context
                );
                break;
                
            case ICON_STYLE_COLOR_ICON_NO_BG:
                int iconColor = increaseSaturation(baseColor, ICON_SATURATION_BOOST);
                icon.setTint(iconColor);
                icon.setTintMode(PorterDuff.Mode.SRC_ATOP);
                preference.setIcon(icon);
                break;
                
            case ICON_STYLE_ACCENT_OUTLINE_ACCENT_ICON:
                applyIconWithOutline(
                    preference, icon,
                    resolveThemeColorAccent(context),
                    Color.TRANSPARENT,
                    cornerRadiusDp,
                    context,
                    true
                );
                break;
                
            case ICON_STYLE_SOLID_OUTLINE_SOLID_ICON:
                int saturatedOutlineColor = increaseSaturation(baseColor, ICON_SATURATION_BOOST);
                applyIconWithOutline(
                    preference, icon,
                    saturatedOutlineColor,
                    Color.TRANSPARENT,
                    cornerRadiusDp,
                    context,
                    false
                );
                break;
                
            case ICON_STYLE_DISABLED:
            default:
                int accentColor = resolveThemeColorAccent(context);
                icon.setTint(accentColor);
                icon.setTintMode(PorterDuff.Mode.SRC_ATOP);
                preference.setIcon(icon);
                break;
        }
    }
    
    private static void applyIconWithBackground(Preference preference, Drawable icon, 
            int baseColor, boolean useAlpha, int iconColor, int cornerRadiusDp, Context context) {
        int bgColor = useAlpha ? 
                ColorUtils.setAlphaComponent(baseColor, (int)(255 * BG_ALPHA)) :
                baseColor;
        
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (BG_PADDING_DP * density);
        int cornerRadius = (int) (cornerRadiusDp * density);
        
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setShape(GradientDrawable.RECTANGLE);
        bgDrawable.setCornerRadius(cornerRadius);
        bgDrawable.setColor(bgColor);
        
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        
        Drawable[] layers = {bgDrawable, icon};
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(1, padding, padding, padding, padding);

        icon.setTint(iconColor);
        icon.setTintMode(PorterDuff.Mode.SRC_ATOP);
        
        preference.setIcon(layerDrawable);
    }
    
    private static void applyIconWithGradientBackground(Preference preference, Drawable icon, 
            int baseColor, int cornerRadiusDp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (BG_PADDING_DP * density);
        int cornerRadius = (int) (cornerRadiusDp * density);
        
        int startColor = adjustSaturation(lightenColor(baseColor, GRADIENT_PASTEL_FACTOR), -GRADIENT_SATURATION_FACTOR);
        int endColor = adjustSaturation(darkenColor(baseColor, GRADIENT_SATURATION_FACTOR), GRADIENT_PASTEL_FACTOR);
        int[] gradientColors = {startColor, baseColor, endColor};
        
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setShape(GradientDrawable.RECTANGLE);
        bgDrawable.setCornerRadius(cornerRadius);
        bgDrawable.setColors(gradientColors);
        bgDrawable.setOrientation(GradientDrawable.Orientation.TL_BR);
        bgDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bgDrawable.setGradientCenter(0.5f, 0.5f);
        
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        
        Drawable[] layers = {bgDrawable, icon};
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(1, padding, padding, padding, padding);

        icon.setTint(Color.WHITE);
        icon.setTintMode(PorterDuff.Mode.SRC_ATOP);
        
        preference.setIcon(layerDrawable);
    }
    
    private static void applyIconWithOutline(Preference preference, Drawable icon,
            int outlineColor, int fillColor, int cornerRadiusDp, Context context,
            boolean useAccentForIcon) {
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (BG_PADDING_DP * density);
        int outlineWidth = (int) (OUTLINE_WIDTH_DP * density);
        int cornerRadius = (int) (cornerRadiusDp * density);
        
        GradientDrawable outlineDrawable = new GradientDrawable();
        outlineDrawable.setShape(GradientDrawable.RECTANGLE);
        outlineDrawable.setCornerRadius(cornerRadius);
        outlineDrawable.setColor(fillColor);
        outlineDrawable.setStroke(outlineWidth, outlineColor);
        
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        
        Drawable[] layers = {outlineDrawable, icon};
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(1, padding, padding, padding, padding);

        int iconColor = useAccentForIcon ? resolveThemeColorAccent(context) : outlineColor;
        icon.setTint(iconColor);
        icon.setTintMode(PorterDuff.Mode.SRC_ATOP);
        
        preference.setIcon(layerDrawable);
    }
    
    @ColorInt
    private static int adjustSaturation(@ColorInt int color, float saturationDelta) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(0f, Math.min(1f, hsv[1] + saturationDelta));
        return Color.HSVToColor(hsv);
    }
    
    private static int resolveThemeColorAccent(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        return typedValue.data;
    }

    private static int getCachedColorForPreference(String key, Resources res) {
        if (colorCache.containsKey(key)) {
            return colorCache.get(key);
        }
        
        int colorIndex = Math.abs(key.hashCode()) % PASTEL_COLOR_RES_IDS.length;
        int color = res.getColor(PASTEL_COLOR_RES_IDS[colorIndex], null);
        colorCache.put(key, color);
        
        return color;
    }

    private static int getRandomPastelColor(Resources res) {
        int colorIndex = random.nextInt(PASTEL_COLOR_RES_IDS.length);
        return res.getColor(PASTEL_COLOR_RES_IDS[colorIndex], null);
    }

    @ColorInt
    private static int increaseSaturation(@ColorInt int color, float saturationBoost) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, hsv[1] + saturationBoost);
        return Color.HSVToColor(hsv);
    }
    
    @ColorInt
    private static int lightenColor(@ColorInt int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1f, hsv[2] + factor);
        return Color.HSVToColor(hsv);
    }
    
    @ColorInt
    private static int darkenColor(@ColorInt int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0f, hsv[2] - factor);
        return Color.HSVToColor(hsv);
    }

    public static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final PreferenceScreen mScreen;

        public SettingsObserver(Handler handler, Context context, PreferenceScreen screen) {
            super(handler);
            mContext = context;
            mScreen = screen;
        }

        public void register() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(ICON_STYLE),
                    false,
                    this,
                    UserHandle.USER_CURRENT);
                    
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(ICON_RANDOM_COLORS),
                    false,
                    this,
                    UserHandle.USER_CURRENT);
                    
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(ICON_CORNER_STYLE),
                    false,
                    this,
                    UserHandle.USER_CURRENT);
        }

        public void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(ICON_STYLE)) || 
                uri.equals(Settings.System.getUriFor(ICON_RANDOM_COLORS)) ||
                uri.equals(Settings.System.getUriFor(ICON_CORNER_STYLE))) {
                colorCache.clear();
                tintIcons(mScreen, mContext);
            }
        }
    }
}
