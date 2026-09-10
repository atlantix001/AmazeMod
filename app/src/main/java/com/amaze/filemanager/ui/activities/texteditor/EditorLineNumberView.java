/*
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.amaze.filemanager.ui.activities.texteditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;

/** Lightweight logical-line gutter that stays aligned with the editor's wrapped text layout. */
public final class EditorLineNumberView extends View {

  private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int horizontalPaddingPx;
  private final int minimumWidthPx;
  private AppCompatEditText editor;

  public EditorLineNumberView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    final float density = getResources().getDisplayMetrics().density;
    horizontalPaddingPx = Math.round(8f * density);
    minimumWidthPx = Math.round(38f * density);

    final int onSurface =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
    numberPaint.setColor(ColorUtils.setAlphaComponent(onSurface, 166));
    numberPaint.setTextAlign(Paint.Align.RIGHT);
    dividerPaint.setColor(ColorUtils.setAlphaComponent(onSurface, 48));
    dividerPaint.setStrokeWidth(Math.max(1f, density));
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  public void setEditor(AppCompatEditText editor) {
    this.editor = editor;
    editor.addOnLayoutChangeListener(
        (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> invalidate());
    refresh();
  }

  public void refresh() {
    requestLayout();
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final float editorTextSize = editor == null ? 14f : editor.getTextSize();
    numberPaint.setTextSize(Math.max(spToPx(10f), editorTextSize * 0.78f));

    final int visualLineCount = editor == null ? 1 : Math.max(1, editor.getLineCount());
    final int digits = Integer.toString(visualLineCount).length();
    final StringBuilder widest = new StringBuilder(digits);
    for (int i = 0; i < digits; i++) {
      widest.append('8');
    }
    final int desiredWidth =
        Math.max(
            minimumWidthPx,
            Math.round(numberPaint.measureText(widest.toString())) + horizontalPaddingPx * 2);
    final int desiredHeight = editor == null ? getSuggestedMinimumHeight() : editor.getMeasuredHeight();
    setMeasuredDimension(
        resolveSize(desiredWidth, widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (editor == null || editor.getLayout() == null || editor.getText() == null) {
      return;
    }

    final Layout layout = editor.getLayout();
    final CharSequence text = editor.getText();
    numberPaint.setTextSize(Math.max(spToPx(10f), editor.getTextSize() * 0.78f));

    int logicalLine = 1;
    for (int visualLine = 0; visualLine < layout.getLineCount(); visualLine++) {
      final int start = layout.getLineStart(visualLine);
      final boolean logicalLineStart =
          visualLine == 0 || (start > 0 && text.charAt(start - 1) == '\n');
      if (!logicalLineStart) {
        continue;
      }

      final float baseline = editor.getCompoundPaddingTop() + layout.getLineBaseline(visualLine);
      canvas.drawText(
          Integer.toString(logicalLine++), getWidth() - horizontalPaddingPx, baseline, numberPaint);
    }

    canvas.drawLine(getWidth() - 1f, 0f, getWidth() - 1f, getHeight(), dividerPaint);
  }

  private float spToPx(float sp) {
    return sp * getResources().getDisplayMetrics().scaledDensity;
  }
}
