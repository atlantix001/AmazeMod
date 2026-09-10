/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.views.appbar;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.adapters.SearchRecyclerViewAdapter;
import com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem.SearchResult;
import com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem.SearchResultListSorter;
import com.amaze.filemanager.filesystem.files.sort.DirSortBy;
import com.amaze.filemanager.filesystem.files.sort.SortBy;
import com.amaze.filemanager.filesystem.files.sort.SortOrder;
import com.amaze.filemanager.filesystem.files.sort.SortType;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.MainActivityViewModel;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.theme.MonetColorHelper;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import kotlinx.coroutines.Job;

/**
 * SearchView, a simple view to search
 *
 * @author Emmanuel on 2/8/2017, at 23:30.
 */
public class SearchView {

  private final MainActivity mainActivity;
  private final AppBar appbar;

  private final NestedScrollView searchViewLayout;
  private final AppCompatEditText searchViewEditText;

  private final AppCompatImageView clearImageView;
  private final AppCompatImageView backImageView;

  private final AppCompatTextView recentHintTV;
  private final AppCompatButton clearHistoryButton;
  private final AppCompatTextView searchResultsHintTV;
  private final AppCompatButton deepSearchButton;
  private final LinearLayout deepSearchContainer;

  private final ChipGroup recentChipGroup;
  private final View recentScrollView;
  private final RecyclerView recyclerView;

  private final SearchRecyclerViewAdapter searchRecyclerViewAdapter;

  /** Text to describe {@link SearchView#searchResultsSortButton} */
  private final AppCompatTextView searchResultsSortHintTV;

  /** The button to select how the results should be sorted */
  private final AppCompatButton searchResultsSortButton;

  /** The drawable used to indicate that the search results are sorted ascending */
  private final Drawable searchResultsSortAscDrawable;

  /** The drawable used to indicate that the search results are sorted descending */
  private final Drawable searchResultsSortDescDrawable;

  // 0 -> Basic Search
  // 1 -> Indexed Search
  // 2 -> Deep Search
  private int searchMode;

  private boolean enabled = false;
  private final int originalSoftInputMode;

  private static final long SEARCH_REVEAL_DURATION_MS = 220L;
  private static final long SEARCH_HIDE_DURATION_MS = 180L;

  private final SortType defaultSortType = new SortType(SortBy.RELEVANCE, SortOrder.ASC);

  /** The selected sort type for the search results */
  private SortType sortType = defaultSortType;

  @SuppressWarnings("ConstantConditions")
  public SearchView(final AppBar appbar, MainActivity mainActivity) {

    this.mainActivity = mainActivity;
    this.appbar = appbar;

    searchViewLayout = mainActivity.findViewById(R.id.search_view);
    searchViewEditText = mainActivity.findViewById(R.id.search_edit_text);
    clearImageView = mainActivity.findViewById(R.id.search_close_btn);
    backImageView = mainActivity.findViewById(R.id.img_view_back);
    recentChipGroup = mainActivity.findViewById(R.id.searchRecentItemsChipGroup);
    recentScrollView = mainActivity.findViewById(R.id.searchRecentItemsScrollView);
    recentHintTV = mainActivity.findViewById(R.id.searchRecentHintTV);
    clearHistoryButton = mainActivity.findViewById(R.id.searchClearHistoryButton);
    searchResultsHintTV = mainActivity.findViewById(R.id.searchResultsHintTV);
    deepSearchButton = mainActivity.findViewById(R.id.tryDeepSearchButton);
    deepSearchContainer = mainActivity.findViewById(R.id.deepSearchContainer);
    recyclerView = mainActivity.findViewById(R.id.searchRecyclerView);
    searchResultsSortHintTV = mainActivity.findViewById(R.id.searchResultsSortHintTV);
    searchResultsSortButton = mainActivity.findViewById(R.id.searchResultsSortButton);
    searchResultsSortAscDrawable =
        ResourcesCompat.getDrawable(
            mainActivity.getResources(),
            R.drawable.baseline_sort_24_asc_white,
            mainActivity.getTheme());
    searchResultsSortDescDrawable =
        ResourcesCompat.getDrawable(
            mainActivity.getResources(),
            R.drawable.baseline_sort_24_desc_white,
            mainActivity.getTheme());
    originalSoftInputMode = mainActivity.getWindow().getAttributes().softInputMode;

    setUpSearchResultsSortButton();

    initRecentSearches(mainActivity);

    searchRecyclerViewAdapter = new SearchRecyclerViewAdapter();
    recyclerView.setAdapter(searchRecyclerViewAdapter);

    clearImageView.setOnClickListener(
        v -> {
          // observers of last search are removed to stop updating the results
          cancelLastSearch();

          searchViewEditText.setText("");
          clearRecyclerView();
        });

    backImageView.setOnClickListener(v -> appbar.getSearchView().hideSearchView());
    clearHistoryButton.setOnClickListener(v -> clearRecentSearches());

    searchViewEditText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (count > 0) searchViewEditText.setError(null);

            if (count >= 3) onSearch(false);
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    searchViewEditText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {

            Utils.hideKeyboard(mainActivity);

            return onSearch(true);
          }

          return false;
        });

    deepSearchButton.setOnClickListener(
        v -> {
          String s = getSearchTerm();

          cancelLastSearch();

          if (searchMode == 1) {

            saveRecentPreference(s);

            mainActivity
                .getCurrentMainFragment()
                .getMainActivityViewModel()
                .indexedSearch(mainActivity, s)
                .observe(
                    mainActivity.getCurrentMainFragment().getViewLifecycleOwner(),
                    hybridFileParcelables -> updateResultList(hybridFileParcelables, s));

            searchMode = 2;

            deepSearchButton.setText(mainActivity.getString(R.string.try_deep_search));

          } else if (searchMode == 2) {

            mainActivity
                .getCurrentMainFragment()
                .getMainActivityViewModel()
                .deepSearch(mainActivity, s)
                .observe(
                    mainActivity.getCurrentMainFragment().getViewLifecycleOwner(),
                    hybridFileParcelables -> updateResultList(hybridFileParcelables, s));

            deepSearchContainer.setVisibility(View.GONE);
          }
        });

    initSearchViewColor(mainActivity);
    applySearchWindowInsets();
  }

  @SuppressWarnings("ConstantConditions")
  private boolean onSearch(boolean shouldSave) {

    String s = getSearchTerm();

    if (s.isEmpty()) {
      searchViewEditText.setError(mainActivity.getString(R.string.field_empty));
      searchViewEditText.requestFocus();
      return false;
    }

    basicSearch(s);

    if (shouldSave) saveRecentPreference(s);

    return true;
  }

  private void basicSearch(String s) {

    clearRecyclerView();

    searchResultsHintTV.setVisibility(View.VISIBLE);
    searchResultsSortButton.setVisibility(View.VISIBLE);
    searchResultsSortHintTV.setVisibility(View.VISIBLE);
    deepSearchContainer.setVisibility(View.VISIBLE);
    searchMode = 1;
    deepSearchButton.setText(mainActivity.getString(R.string.try_indexed_search));

    mainActivity
        .getCurrentMainFragment()
        .getMainActivityViewModel()
        .basicSearch(mainActivity, s)
        .observe(
            mainActivity.getCurrentMainFragment().getViewLifecycleOwner(),
            hybridFileParcelables -> updateResultList(hybridFileParcelables, s));
  }

  private void saveRecentPreference(String s) {

    String preferenceString =
        PreferenceManager.getDefaultSharedPreferences(mainActivity)
            .getString(PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, null);

    ArrayList<String> recentSearches =
        preferenceString != null
            ? new Gson().fromJson(preferenceString, new TypeToken<ArrayList<String>>() {}.getType())
            : new ArrayList<>();

    if (s.isEmpty() || recentSearches.contains(s)) return;

    recentSearches.add(s);

    if (recentSearches.size() > 5) recentSearches.remove(0);

    PreferenceManager.getDefaultSharedPreferences(mainActivity)
        .edit()
        .putString(
            PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, new Gson().toJson(recentSearches))
        .apply();

    initRecentSearches(mainActivity);
  }

  private void initRecentSearches(Context context) {

    String preferenceString =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, null);

    if (preferenceString == null) {
      setRecentSearchesVisible(false);
      return;
    }

    recentChipGroup.removeAllViews();

    ArrayList<String> recentSearches =
        new Gson().fromJson(preferenceString, new TypeToken<ArrayList<String>>() {}.getType());
    if (recentSearches == null || recentSearches.isEmpty()) {
      setRecentSearchesVisible(false);
      return;
    }

    setRecentSearchesVisible(true);

    for (String string : recentSearches) {
      Chip chip = new Chip(new ContextThemeWrapper(context, R.style.ChipStyle));

      chip.setText(string);

      recentChipGroup.addView(chip);

      chip.setOnClickListener(
          v -> {
            String s = ((Chip) v).getText().toString();

            searchViewEditText.setText(s);

            Utils.hideKeyboard(mainActivity);

            basicSearch(s);
          });
    }
  }

  private void clearRecentSearches() {
    PreferenceManager.getDefaultSharedPreferences(mainActivity)
        .edit()
        .remove(PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS)
        .apply();
    recentChipGroup.removeAllViews();
    setRecentSearchesVisible(false);
  }

  private void setRecentSearchesVisible(boolean visible) {
    int visibility = visible ? View.VISIBLE : View.GONE;
    recentHintTV.setVisibility(visibility);
    recentChipGroup.setVisibility(visibility);
    recentScrollView.setVisibility(visibility);
    clearHistoryButton.setVisibility(visibility);
  }

  private void resetSearchMode() {
    searchMode = 0;
    deepSearchButton.setText(mainActivity.getString(R.string.try_indexed_search));
    deepSearchContainer.setVisibility(View.GONE);
  }

  /**
   * Updates the list of results displayed in {@link SearchView#searchRecyclerViewAdapter} sorted
   * according to the current {@link SearchView#sortType}
   *
   * @param newResults The list of results that should be displayed
   * @param searchTerm The search term that resulted in the search results
   */
  private void updateResultList(List<SearchResult> newResults, String searchTerm) {
    if (newResults != null) {
      ArrayList<SearchResult> items = new ArrayList<>(newResults);
      Collections.sort(
          items, new SearchResultListSorter(DirSortBy.NONE_ON_TOP, sortType, searchTerm));
      searchRecyclerViewAdapter.submitList(items);
    } else {
      AppConfig.toast(mainActivity, "No search result found");
    }
  }

  /** show search view with a circular reveal animation */
  public void revealSearchView() {
    final int START_RADIUS = 16;
    int endRadius = Math.max(appbar.getToolbar().getWidth(), appbar.getToolbar().getHeight());

    resetSearchMode();
    resetSearchResultsSortButton();
    clearRecyclerView();

    Animator animator;
    if (SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      int[] searchCoords = new int[2];
      View searchItem =
          appbar
              .getToolbar()
              .findViewById(R.id.search); // It could change position, get it every time
      searchViewEditText.setText("");
      searchItem.getLocationOnScreen(searchCoords);
      animator =
          ViewAnimationUtils.createCircularReveal(
              searchViewLayout,
              searchCoords[0] + 32,
              searchCoords[1] - 16,
              START_RADIUS,
              endRadius);
    } else {
      // TODO:ViewAnimationUtils.createCircularReveal
      animator = ObjectAnimator.ofFloat(searchViewLayout, "alpha", 0f, 1f);

      searchViewLayout.bringToFront(); // since android:elevation won't work
      searchViewEditText.requestFocus(); // for keyboard auto-popup
    }

    mainActivity.showSmokeScreen();
    mainActivity.hideFab();

    // MainActivity normally uses adjustPan. During the full-screen search surface, adjustResize
    // avoids the whole window jumping when the IME appears. Start the keyboard together with the
    // much shorter reveal animation instead of waiting 600 ms for the reveal to finish first.
    mainActivity
        .getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(SEARCH_REVEAL_DURATION_MS);
    searchViewLayout.setVisibility(View.VISIBLE);
    searchViewEditText.requestFocus();
    enabled = true;
    searchViewEditText.post(
        () -> {
          InputMethodManager imm =
              (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.showSoftInput(searchViewEditText, InputMethodManager.SHOW_IMPLICIT);
        });
    animator.start();
    animator.addListener(
        new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animation) {}

          @Override
          public void onAnimationEnd(Animator animation) {}

          @Override
          public void onAnimationCancel(Animator animation) {}

          @Override
          public void onAnimationRepeat(Animator animation) {}
        });
  }

  /**
   * Sets up the {@link SearchView#searchResultsSortButton} to show a dialog when it is clicked. The
   * text and icon of {@link SearchView#searchResultsSortButton} is also set to the current {@link
   * SearchView#sortType}
   */
  private void setUpSearchResultsSortButton() {
    searchResultsSortButton.setOnClickListener(v -> showSearchResultsSortDialog());
    updateSearchResultsSortButtonDisplay();
  }

  /** Builds and shows a Material Components dialog for search-result sorting. */
  private void showSearchResultsSortDialog() {
    final int[] selectedIndex = {sortType.getSortBy().getIndex()};
    new MaterialAlertDialogBuilder(mainActivity)
        .setTitle(R.string.sort_by)
        .setSingleChoiceItems(
            R.array.sortbySearch,
            selectedIndex[0],
            (dialog, which) -> selectedIndex[0] = which)
        .setPositiveButton(
            R.string.ascending,
            (dialog, which) -> onSortTypeSelected(selectedIndex[0], SortOrder.ASC))
        .setNegativeButton(
            R.string.descending,
            (dialog, which) -> onSortTypeSelected(selectedIndex[0], SortOrder.DESC))
        .show();
  }

  private void onSortTypeSelected(int index, SortOrder sortOrder) {
    this.sortType = new SortType(SortBy.getSortBy(index), sortOrder);
    updateSearchResultsSortButtonDisplay();
    LiveData<List<SearchResult>> lastSearchLiveData =
        mainActivity.getCurrentMainFragment().getMainActivityViewModel().getLastSearchLiveData();
    updateResultList(lastSearchLiveData.getValue(), getSearchTerm());
  }

  private void resetSearchResultsSortButton() {
    sortType = defaultSortType;
    updateSearchResultsSortButtonDisplay();
  }

  /** Updates the text and icon of {@link SearchView#searchResultsSortButton} */
  private void updateSearchResultsSortButtonDisplay() {
    searchResultsSortButton.setText(sortType.getSortBy().toResourceString(mainActivity));
    setSearchResultSortOrderIcon();
  }

  /**
   * Updates the icon of {@link SearchView#searchResultsSortButton} and colors it to fit the text
   * color
   */
  private void setSearchResultSortOrderIcon() {
    Drawable orderDrawable;
    switch (sortType.getSortOrder()) {
      default:
      case ASC:
        orderDrawable = searchResultsSortAscDrawable;
        break;
      case DESC:
        orderDrawable = searchResultsSortDescDrawable;
        break;
    }

    orderDrawable.setColorFilter(
        new PorterDuffColorFilter(
            mainActivity.getAccent(),
            PorterDuff.Mode.SRC_ATOP));
    searchResultsSortButton.setCompoundDrawablesWithIntrinsicBounds(
        null, null, orderDrawable, null);
  }

  /** hide search view with a circular reveal animation */
  public void hideSearchView() {
    final int END_RADIUS = 16;
    int startRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());
    Animator animator;
    if (SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      int[] searchCoords = new int[2];
      View searchItem =
          appbar
              .getToolbar()
              .findViewById(R.id.search); // It could change position, get it every time
      searchViewEditText.setText("");
      searchItem.getLocationOnScreen(searchCoords);
      animator =
          ViewAnimationUtils.createCircularReveal(
              searchViewLayout,
              searchCoords[0] + 32,
              searchCoords[1] - 16,
              startRadius,
              END_RADIUS);
    } else {
      // TODO: ViewAnimationUtils.createCircularReveal
      animator = ObjectAnimator.ofFloat(searchViewLayout, "alpha", 1f, 0f);
    }

    clearRecyclerView();

    // removing background fade view
    mainActivity.hideSmokeScreen();
    mainActivity.showFab();
    InputMethodManager inputMethodManager =
        (InputMethodManager) mainActivity.getSystemService(INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(
        searchViewEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(SEARCH_HIDE_DURATION_MS);
    animator.start();
    animator.addListener(
        new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animation) {}

          @Override
          public void onAnimationEnd(Animator animation) {
            searchViewLayout.setVisibility(View.GONE);
            enabled = false;
            mainActivity.getWindow().setSoftInputMode(originalSoftInputMode);
          }

          @Override
          public void onAnimationCancel(Animator animation) {}

          @Override
          public void onAnimationRepeat(Animator animation) {}
        });
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isShown() {
    return searchViewLayout.isShown();
  }

  private void initSearchViewColor(MainActivity a) {
    searchViewLayout.setBackgroundResource(R.drawable.search_view_shape);
    int onSurface =
        MaterialColors.getColor(
            searchViewLayout, com.google.android.material.R.attr.colorOnSurface);
    searchViewEditText.setTextColor(onSurface);
    searchViewEditText.setHintTextColor(ColorUtils.setAlphaComponent(onSurface, 0x99));
    clearImageView.setColorFilter(onSurface, PorterDuff.Mode.SRC_ATOP);
    backImageView.setColorFilter(onSurface, PorterDuff.Mode.SRC_ATOP);
  }

  /** Keep the full-screen search surface clear of status/navigation bars and display cutouts. */
  private void applySearchWindowInsets() {
    if (!MonetColorHelper.isEnabled(mainActivity, mainActivity.getPrefs())) {
      return;
    }
    ViewGroup.LayoutParams rawParams = searchViewLayout.getLayoutParams();
    if (!(rawParams instanceof ViewGroup.MarginLayoutParams)) {
      return;
    }
    ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) rawParams;
    final int baseLeft = margins.leftMargin;
    final int baseTop = margins.topMargin;
    final int baseRight = margins.rightMargin;
    final int baseBottom = margins.bottomMargin;

    ViewCompat.setOnApplyWindowInsetsListener(
        searchViewLayout,
        (view, windowInsets) -> {
          Insets safeInsets =
              windowInsets.getInsets(
                  WindowInsetsCompat.Type.statusBars()
                      | WindowInsetsCompat.Type.navigationBars()
                      | WindowInsetsCompat.Type.displayCutout());
          ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
          lp.leftMargin = baseLeft + safeInsets.left;
          lp.topMargin = baseTop + safeInsets.top;
          lp.rightMargin = baseRight + safeInsets.right;
          lp.bottomMargin = baseBottom + safeInsets.bottom;
          view.setLayoutParams(lp);
          return windowInsets;
        });
    ViewCompat.requestApplyInsets(searchViewLayout);
  }

  private void clearRecyclerView() {
    searchRecyclerViewAdapter.submitList(Collections.emptyList());

    deepSearchContainer.setVisibility(View.GONE);

    searchResultsHintTV.setVisibility(View.GONE);
    searchResultsSortHintTV.setVisibility(View.GONE);
    searchResultsSortButton.setVisibility(View.GONE);
  }

  private SpannableString getSpannableText(String s1, String s2) {

    SpannableString spannableString = new SpannableString(s1 + " " + s2);

    spannableString.setSpan(
        new ForegroundColorSpan(mainActivity.getCurrentColorPreference().getAccent()),
        s1.length() + 1,
        spannableString.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannableString.setSpan(
        new StyleSpan(Typeface.BOLD),
        s1.length() + 1,
        spannableString.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spannableString;
  }

  /**
   * Returns the current text in {@link SearchView#searchViewEditText}
   *
   * @return The current search text
   */
  private String getSearchTerm() {
    return searchViewEditText.getText().toString().trim();
  }

  private void cancelLastSearch() {
    MainActivityViewModel viewModel =
        mainActivity.getCurrentMainFragment().getMainActivityViewModel();

    // remove all observers
    viewModel
        .getLastSearchLiveData()
        .removeObservers(mainActivity.getCurrentMainFragment().getViewLifecycleOwner());

    // stop the job
    Job lastJob = viewModel.getLastSearchJob();
    if (lastJob != null) {
      lastJob.cancel(new CancellationException("Search outdated"));
    }
  }
}
