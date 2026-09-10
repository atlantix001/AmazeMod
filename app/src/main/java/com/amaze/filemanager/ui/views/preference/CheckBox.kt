/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.views.preference

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.amaze.filemanager.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Amaze switch preference using a Material switch widget while keeping the existing preference
 * persistence and click behaviour.
 */
class CheckBox(context: Context, attrs: AttributeSet) : SwitchPreferenceCompat(context, attrs) {
    init {
        widgetLayoutResource = R.layout.preference_widget_material_switch
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        clearListenerInViewGroup(holder.itemView as ViewGroup)
        super.onBindViewHolder(holder)

        val materialSwitch = holder.findViewById(R.id.switchWidget) as? MaterialSwitch ?: return
        centreWidgetFrame(materialSwitch)
        applyAmazeSwitchColors(materialSwitch)
    }

    /** Clear the stale listener before SwitchPreferenceCompat rebinds the widget. */
    private fun clearListenerInViewGroup(viewGroup: ViewGroup) {
        for (n in 0 until viewGroup.childCount) {
            val childView = viewGroup.getChildAt(n)
            if (childView is CompoundButton) {
                childView.setOnCheckedChangeListener(null)
                return
            } else if (childView is ViewGroup) {
                clearListenerInViewGroup(childView)
            }
        }
    }

    /**
     * AndroidX Preference's widget frame can align to the title block rather than the complete row
     * when a preference has no summary or a multi-line summary. Force the switch column itself to
     * stay vertically centred so all preference rows line up consistently.
     */
    private fun centreWidgetFrame(materialSwitch: MaterialSwitch) {
        val root = materialSwitch.parent as? View ?: return
        val widgetFrame = root.parent as? View ?: return
        val params = widgetFrame.layoutParams
        if (params is LinearLayout.LayoutParams) {
            params.gravity = Gravity.CENTER_VERTICAL
            widgetFrame.layoutParams = params
        }
    }

    /** Keep Material3 controls on Amaze's runtime accent instead of a fallback theme palette. */
    private fun applyAmazeSwitchColors(materialSwitch: MaterialSwitch) {
        val accent =
            MaterialColors.getColor(
                materialSwitch,
                androidx.appcompat.R.attr.colorAccent,
            )
        val surface =
            MaterialColors.getColor(
                materialSwitch,
                com.google.android.material.R.attr.colorSurface,
            )
        val onSurface =
            MaterialColors.getColor(
                materialSwitch,
                com.google.android.material.R.attr.colorOnSurface,
            )
        val checkedThumb =
            if (ColorUtils.calculateContrast(Color.BLACK, accent) >=
                ColorUtils.calculateContrast(Color.WHITE, accent)
            ) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        val uncheckedTrack = ColorUtils.blendARGB(surface, onSurface, 0.28f)
        val uncheckedThumb = ColorUtils.blendARGB(surface, onSurface, 0.82f)
        val states =
            arrayOf(
                intArrayOf(android.R.attr.state_checked, android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(),
            )

        materialSwitch.trackTintList =
            ColorStateList(
                states,
                intArrayOf(
                    accent,
                    ColorUtils.setAlphaComponent(accent, 0x61),
                    uncheckedTrack,
                    ColorUtils.setAlphaComponent(uncheckedTrack, 0x61),
                ),
            )
        materialSwitch.thumbTintList =
            ColorStateList(
                states,
                intArrayOf(
                    checkedThumb,
                    ColorUtils.setAlphaComponent(checkedThumb, 0xB3),
                    uncheckedThumb,
                    ColorUtils.setAlphaComponent(uncheckedThumb, 0x61),
                ),
            )
    }
}
