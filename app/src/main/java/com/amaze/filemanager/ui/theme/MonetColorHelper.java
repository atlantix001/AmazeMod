/*
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.amaze.filemanager.ui.theme;

import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/** Android 12+ system dynamic color (Monet) bridge for Amaze's legacy color model. */
public final class MonetColorHelper {

  private MonetColorHelper() {}

  public static boolean isAvailable() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
  }

  public static boolean isEnabled(
      @NonNull Context context, @NonNull SharedPreferences preferences) {
    return isAvailable()
        && preferences.getBoolean(PreferencesConstants.PREFERENCE_MONET_DYNAMIC_COLORS, true);
  }

  @NonNull
  public static UserColorPreferences getColors(@NonNull Context context, @NonNull AppTheme theme) {
    if (!isAvailable()) {
      throw new IllegalStateException("Dynamic system colors require Android 12 or newer");
    }

    // Amaze's legacy color model uses one primary color for large app-bar surfaces and a separate
    // accent for controls. Keep the app bar visibly tonal instead of collapsing to near-black on
    // dark wallpapers, while retaining enough contrast for its fixed white toolbar content.
    final boolean lightTheme = theme == AppTheme.LIGHT;
    @ColorInt
    int primary =
        ContextCompat.getColor(
            context,
            lightTheme
                ? android.R.color.system_accent1_500
                : android.R.color.system_accent1_600);
    primary = ensureWhiteContentContrast(primary);

    @ColorInt
    int accent =
        ContextCompat.getColor(
            context,
            lightTheme
                ? android.R.color.system_accent1_600
                : android.R.color.system_accent1_200);
    @ColorInt
    int icon =
        ContextCompat.getColor(
            context,
            lightTheme
                ? android.R.color.system_accent1_600
                : android.R.color.system_accent1_200);

    // Amaze supports a separate primary color per pane. Monet intentionally keeps both panes on
    // the same system-derived primary palette so the UI remains visually coherent.
    return new UserColorPreferences(primary, primary, accent, icon);
  }

  /** Resolve the accent used outside Activity theme contexts (for example foreground services). */
  @ColorInt
  public static int resolveAccent(
      @NonNull Context context,
      @NonNull SharedPreferences preferences,
      @NonNull AppTheme theme,
      @ColorInt int fallback) {
    return isEnabled(context, preferences) ? getColors(context, theme).getAccent() : fallback;
  }

  @ColorInt
  private static int ensureWhiteContentContrast(@ColorInt int color) {
    int adjusted = color;
    int steps = 0;
    while (ColorUtils.calculateContrast(android.graphics.Color.WHITE, adjusted) < 3.25
        && steps++ < 8) {
      adjusted = ColorUtils.blendARGB(adjusted, android.graphics.Color.BLACK, 0.05f);
    }
    return adjusted;
  }
}
