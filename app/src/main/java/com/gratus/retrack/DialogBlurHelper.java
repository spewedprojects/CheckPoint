package com.gratus.retrack;

import android.app.Activity;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

public class DialogBlurHelper {

    private final Activity activity;
    private View blurredView;
    private float blurRadius;

    /**
     * @param activity The host activity.
     * @param blurRadius The intensity of the blur (e.g., 10f, 15f).
     */
    public DialogBlurHelper(Activity activity, float blurRadius) {
        this.activity = activity;
        this.blurRadius = blurRadius;
    }

    public void applyBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurredView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
            if (blurredView != null) {
                blurredView.setRenderEffect(
                        RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.DECAL)
                );
            }
        }
    }

    public void removeBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurredView != null) {
                blurredView.setRenderEffect(null);
            }
        }
    }
}