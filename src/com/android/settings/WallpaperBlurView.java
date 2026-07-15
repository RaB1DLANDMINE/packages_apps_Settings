/*
 * Copyright (C) 2021 Project Radiant
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

package com.android.settings;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;

public class WallpaperBlurView extends androidx.appcompat.widget.AppCompatImageView {

    private Context context;
    private boolean isBlurred = true;
    private static final String PREFS_NAME = "WallpaperBlurViewPrefs";
    private static final String KEY_BLUR_STATE = "blurState";

    public WallpaperBlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public WallpaperBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public WallpaperBlurView(Context context) {
        super(context);
        initialize(context);
    }

    private void initialize(Context context) {
        this.context = context;
        loadBlurState();
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleBlur();
                saveBlurState();
                return true;
            }
        });
        updateWallpaper();
    }

    private void updateWallpaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Bitmap wallpaperBitmap = wallpaperManager.getBitmap();
        setImageBitmap(wallpaperBitmap);
        requestLayout();
        invalidate();
    }

    private void toggleBlur() {
        if (isBlurred) {
            setRenderEffect(null);
        } else {
            setRenderEffect(RenderEffect.createBlurEffect(400f, 400f, Shader.TileMode.CLAMP));
        }
        isBlurred = !isBlurred;
    }

    private void saveBlurState() {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_BLUR_STATE, isBlurred);
        editor.apply();
    }

    private void loadBlurState() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isBlurred = prefs.getBoolean(KEY_BLUR_STATE, false);
        if (isBlurred) {
            setRenderEffect(RenderEffect.createBlurEffect(400f, 400f, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateWallpaper();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
