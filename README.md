# Amaze File Manager custom

This branch is the fully materialized source tree of the last known-good custom build:

**Amaze File Manager v3.11.3 (`1d7520eab2ccdecee3c5fda9da440e97c48dd95f`)

From v0.3.0 onward the repository builds the checked-in source directly. There is no runtime patch chain, overlay step, or source transformation step.

The repository now contains the current UI customizations directly in source: the duplicate hidden-files overflow action is removed, interactive dark-theme accents are kept light, and grid/list thumbnails use a consistent rounded treatment.

Signing uses the private `signing/` directory in this repository, matching the proven v0.2.17 setup. See `SIGNING.md`.

Provenance is documented in `UPSTREAM.md` and `LEGACY_PATCH_BASE.md`. The original upstream README is retained as `README-UPSTREAM.md`.
