/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper
import com.amaze.filemanager.ui.fragments.preferencefragments.BasePrefsFragment
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.fragments.preferencefragments.PrefsFragment
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.ui.theme.MonetColorHelper
import com.amaze.filemanager.utils.PreferenceUtils
import com.amaze.filemanager.utils.Utils
import com.readystatesoftware.systembartint.SystemBarTintManager
import java.io.File

class PreferencesActivity : ThemedActivity(), FolderChooserDialog.FolderCallback {
    private companion object {
        const val SAVED_INSTANCE_STATE_KEY = "savedInstanceState"
    }

    lateinit var layout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        var savedInstanceState = savedInstanceState
        if (savedInstanceState == null && intent.hasExtra(SAVED_INSTANCE_STATE_KEY)) {
            savedInstanceState = intent.getBundleExtra(SAVED_INSTANCE_STATE_KEY)
        }

        super.onCreate(savedInstanceState)
        // Preference rows should use the same neutral grey press feedback as Android settings.
        // This overlay changes only colorControlHighlight; switches keep their runtime accent.
        theme.applyStyle(
            if (appTheme == AppTheme.LIGHT) {
                R.style.PreferenceInteractionOverlayLight
            } else {
                R.style.PreferenceInteractionOverlayDark
            },
            true,
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_preferences)

        layout = findViewById(R.id.activity_preferences)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        applySafeWindowInsets(layout, toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.displayOptions =
            ActionBar.DISPLAY_HOME_AS_UP or ActionBar.DISPLAY_SHOW_TITLE
        initStatusBarResources(layout)

        if (savedInstanceState == null) {
            val fragment = PrefsFragment()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.preferences_container, fragment)
                .commit()
        }
    }

    private fun applySafeWindowInsets(
        root: View,
        toolbar: Toolbar,
    ) {
        val rootPaddingLeft = root.paddingLeft
        val rootPaddingTop = root.paddingTop
        val rootPaddingRight = root.paddingRight
        val rootPaddingBottom = root.paddingBottom
        val toolbarPaddingLeft = toolbar.paddingLeft
        val toolbarPaddingTop = toolbar.paddingTop
        val toolbarPaddingRight = toolbar.paddingRight
        val toolbarPaddingBottom = toolbar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val safeInsets =
                windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout(),
                )

            // Keep the app-bar background edge-to-edge while moving its controls below the
            // status bar / camera cutout. Side and bottom insets protect the whole settings UI.
            root.setPadding(
                rootPaddingLeft + safeInsets.left,
                rootPaddingTop,
                rootPaddingRight + safeInsets.right,
                rootPaddingBottom + safeInsets.bottom,
            )
            toolbar.setPadding(
                toolbarPaddingLeft,
                toolbarPaddingTop + safeInsets.top,
                toolbarPaddingRight,
                toolbarPaddingBottom,
            )

            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.action = Intent.CATEGORY_LAUNCHER
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            false
        }
    }

    override fun recreate() {
        val bundle = Bundle()
        onSaveInstanceState(bundle)
        val intent = Intent(this, javaClass)
        intent.putExtra(SAVED_INSTANCE_STATE_KEY, bundle)

        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * Push a new fragment into the stack
     */
    fun pushFragment(fragment: BasePrefsFragment) {
        supportFragmentManager.commit {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

            replace(R.id.preferences_container, fragment)
            supportActionBar?.title = getString(fragment.title)
            addToBackStack(null)
        }
    }

    /**
     * Rebuild the nav bar
     *
     * Used to update color
     */
    fun invalidateNavBar() {
        val primaryColor =
            ColorPreferenceHelper
                .getPrimary(currentColorPreference, MainActivity.currentTab)
        if (Build.VERSION.SDK_INT == 20 || Build.VERSION.SDK_INT == 19) {
            val tintManager = SystemBarTintManager(this)
            tintManager.isStatusBarTintEnabled = true
            tintManager.setStatusBarTintColor(primaryColor)
            val layoutParams =
                findViewById<View>(R.id.activity_preferences).layoutParams
                    as ViewGroup.MarginLayoutParams
            val config = tintManager.config
            layoutParams.setMargins(0, config.statusBarHeight, 0, 0)
        } else if (Build.VERSION.SDK_INT >= 21) {
            val colouredNavigation = getBoolean(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            val tabStatusColor =
                if (MonetColorHelper.isEnabled(this, prefs)) {
                    primaryColor
                } else {
                    PreferenceUtils.getStatusColor(primaryColor)
                }
            window.statusBarColor = tabStatusColor
            when {
                colouredNavigation -> {
                    window.navigationBarColor = tabStatusColor
                }
                appTheme == AppTheme.BLACK -> {
                    window.navigationBarColor = Color.BLACK
                }
                appTheme == AppTheme.DARK -> {
                    window.navigationBarColor = Utils.getColor(this, R.color.holo_dark_background)
                }
                appTheme == AppTheme.LIGHT -> {
                    window.navigationBarColor = Color.WHITE
                }
            }
        }
        if (appTheme == AppTheme.BLACK) {
            window.decorView.setBackgroundColor(Utils.getColor(this, android.R.color.black))
        }
    }

    override fun onFolderSelection(
        dialog: FolderChooserDialog,
        folder: File,
    ) {
        supportFragmentManager.fragments.lastOrNull { it is BasePrefsFragment }?.let {
            (it as BasePrefsFragment).onFolderSelection(dialog, folder)
        }
    }

    override fun onFolderChooserDismissed(dialog: FolderChooserDialog) {
        supportFragmentManager.fragments.lastOrNull { it is BasePrefsFragment }?.let {
            (it as BasePrefsFragment).onFolderChooserDismissed(dialog)
        }
    }
}
