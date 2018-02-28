package com.example.sumimoto.eightcardanimationexample;

import android.content.Context;

public class ViewUtils {

    public static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int dpToPx(Context context, double dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static float dpToPxFloat(Context context, int dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static float spToPxFloat(Context context, int sp) {
        return sp * context.getResources().getDisplayMetrics().scaledDensity;
    }

}
