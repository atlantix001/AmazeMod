# Amaze File Manager custom — v0.3.3 direct source

This branch is the fully materialized source tree of the last known-good custom build:

**Amaze File Manager v3.11.3 (`1d7520eab2ccdecee3c5fda9da440e97c48dd95f`) + AmazeMod v0.2.17.**

From v0.3.0 onward the repository builds the checked-in source directly. There is no runtime patch chain, overlay step, or source transformation step.

The repository now contains the current UI customizations directly in source: the duplicate hidden-files overflow action is removed, interactive dark-theme accents are kept light, and grid/list thumbnails use a consistent rounded treatment.

### v0.3.3

- Unified circular thumbnail surfaces in grid/table and list previews; permissions are placed on a separate centered line above the circle, and stale/recycled Glide callbacks are ignored so thumbnails cannot disappear.
- Properties storage charts use a smaller center label, larger high-contrast slice values, shorter leader lines and a larger chart area for readability.
- Dialogs are modernized globally: legacy MaterialDialogs get 28dp corners and tonal pill actions, Material/AppCompat alerts share the rounded theme, and the remaining SSH/SFTP native alerts are migrated to MaterialAlertDialogBuilder.

### v0.3.2

- fixes recursive size calculation for root-only directories such as `/system` and `/system/app`;
- simplifies the properties pie-chart center label and improves its legend layout;
- reduces thumbnail corner rounding from 24dp to 12dp;
- centers Unix permission strings inside grid/table thumbnails.

Signing uses the private `signing/` directory in this repository, matching the proven v0.2.17 setup. See `SIGNING.md`.

Provenance is documented in `UPSTREAM.md` and `LEGACY_PATCH_BASE.md`. The original upstream README is retained as `README-UPSTREAM.md`.
