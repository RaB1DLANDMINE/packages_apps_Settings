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

package com.android.settings.homepage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Glass UI (Settings): a sleek, slowly flowing colour-mesh gradient drawn behind the homepage
 * content so the translucent glass cards sit on something alive and modern. A full-coverage
 * indigo->cyan base gradient is enriched by three drifting colour blobs (violet / pink / cyan)
 * and finished with a whisper of animated grain so it keeps a tactile, non-flat feel. Gated
 * entirely by the caller - when the {@code glass_ui_settings} toggle is off the view is GONE and
 * does no work. Defensive: if the runtime shader is unavailable for any reason it simply draws
 * nothing (never crashes Settings).
 */
public class GlassNoiseView extends View {

    // Overall layer opacity. Higher than the old grain so the colour scroll reads as a real
    // backdrop behind the cards, but still low enough to keep text legible.
    private static final float GRADIENT_OPACITY = 0.42f;
    // uTime is fed raw seconds; all motion speeds live inside the shader.
    private static final float DRIFT_SPEED = 3.0f;
    // Redraw cadence. A slow aurora drift reads fine well below the panel's native refresh, and
    // every frame costs a full-screen shader + blur GPU pass, so we cap to ~20fps (50ms). Motion
    // stays smooth because uTime is driven by real uptime, independent of how often we repaint.
    private static final long FRAME_INTERVAL_MS = 50L;

    // Northern-lights aurora: rippling vertical curtains of light that drift and shimmer, with a
    // green core, teal mid-tones and violet/magenta fringes over a dark night base.
    private static final String AGSL_GRADIENT = ""
            + "uniform float uTime;"
            + "uniform float uOpacity;"
            + "uniform float2 uResolution;"
            + "float hash(float2 p) {"
            + "    p = fract(p * float2(123.34, 456.21));"
            + "    p += dot(p, p + 45.32);"
            + "    return fract(p.x * p.y);"
            + "}"
            + "float vnoise(float2 p) {"
            + "    float2 i = floor(p);"
            + "    float2 f = fract(p);"
            + "    float a = hash(i);"
            + "    float b = hash(i + float2(1.0, 0.0));"
            + "    float c = hash(i + float2(0.0, 1.0));"
            + "    float d = hash(i + float2(1.0, 1.0));"
            + "    float2 u = f * f * (3.0 - 2.0 * f);"
            + "    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);"
            + "}"
            + "float fbm(float2 p) {"
            + "    return vnoise(p) * 0.6 + vnoise(p * 2.03 + 4.1) * 0.3"
            + "         + vnoise(p * 4.1 + 7.3) * 0.1;"
            + "}"
            + "half4 main(float2 fragCoord) {"
            + "    float2 uv = fragCoord / uResolution;"
            + "    float t = uTime;"
            // warp x by height+time so the curtains wave like real aurora
            + "    float warp = fbm(float2(uv.y * 2.2, t * 0.05)) * 0.55"
            + "               + fbm(float2(uv.y * 4.5 + 3.0, t * 0.035)) * 0.25;"
            + "    float x = uv.x * 2.4 + warp;"
            // vertical ribbons: a few drifting curtains, sharpened into bright bands
            + "    float bands = sin(x * 6.2831 + t * 0.22) * 0.5 + 0.5;"
            + "    float ribbon = pow(bands, 2.2);"
            // fine vertical shimmer streaking up the curtains
            + "    float shimmer = fbm(float2(x * 3.5, uv.y * 3.0 - t * 0.35));"
            + "    ribbon *= (0.65 + 0.5 * shimmer);"
            // vertical envelope: aurora glows in the upper-middle, fades to the horizon
            + "    float env = smoothstep(0.02, 0.45, uv.y) * (1.0 - smoothstep(0.65, 1.08, uv.y));"
            + "    float glow = clamp(ribbon * env, 0.0, 1.0);"
            // aurora palette
            + "    float3 night  = float3(0.04, 0.06, 0.12);"
            + "    float3 green  = float3(0.20, 0.98, 0.55);"
            + "    float3 teal   = float3(0.10, 0.72, 0.78);"
            + "    float3 violet = float3(0.55, 0.28, 0.95);"
            + "    float3 magenta= float3(0.95, 0.35, 0.70);"
            // green core blends to teal in weaker areas
            + "    float3 aur = mix(teal, green, smoothstep(0.2, 0.9, glow));"
            // violet toward the top of the curtains, magenta toward the lower fringe
            + "    aur = mix(aur, violet, smoothstep(0.55, 0.95, uv.y) * 0.6);"
            + "    aur = mix(aur, magenta, smoothstep(0.35, 0.02, uv.y) * 0.35);"
            // composite the glow over the dark night base
            + "    float3 col = mix(night, aur, glow);"
            + "    col *= (0.6 + 0.9 * glow);"  // ribbons bloom brighter
            + "    float a = uOpacity;"
            + "    return half4(half3(col * a), half(a));"  // premultiplied
            + "}";

    private RuntimeShader mShader;
    private final Paint mPaint = new Paint();
    private long mStartMs = SystemClock.uptimeMillis();
    private boolean mAnimating;

    public GlassNoiseView(Context context) {
        this(context, null);
    }

    public GlassNoiseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        try {
            mShader = new RuntimeShader(AGSL_GRADIENT);
            mShader.setFloatUniform("uOpacity", GRADIENT_OPACITY);
        } catch (Throwable t) {
            // No runtime shader support -> stay a no-op, don't take Settings down with us.
            mShader = null;
        }
        // A light blur softens the aurora curtains without mushing them - they should still read
        // as distinct rippling ribbons of light. Cheap: one GPU pass over a flat view.
        try {
            final float r = 18f * getResources().getDisplayMetrics().density;
            setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                    r, r, android.graphics.Shader.TileMode.CLAMP));
        } catch (Throwable t) {
            // RenderEffect unavailable -> unblurred aurora, still fine.
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mShader == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        final float time = (SystemClock.uptimeMillis() - mStartMs) / 1000f * DRIFT_SPEED;
        mShader.setFloatUniform("uTime", time);
        // Required: the shader normalises fragCoord by resolution; an unset declared uniform throws.
        mShader.setFloatUniform("uResolution", getWidth(), getHeight());
        mPaint.setShader(mShader);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), mPaint);
        if (mAnimating) {
            postInvalidateDelayed(FRAME_INTERVAL_MS);
        }
    }

    private void updateAnimating() {
        final boolean shouldAnimate = mShader != null && isShown()
                && getVisibility() == VISIBLE && isAttachedToWindow();
        if (shouldAnimate && !mAnimating) {
            mAnimating = true;
            postInvalidateOnAnimation();
        } else {
            mAnimating = shouldAnimate;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimating();
    }

    @Override
    protected void onDetachedFromWindow() {
        mAnimating = false;
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        updateAnimating();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateAnimating();
    }
}
