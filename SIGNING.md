# Private APK signing

This private repository intentionally keeps the existing signing material in `signing/`, matching the proven v0.2.17 setup.

The direct build reads:

- `signing/keystore.properties`
- the keystore file referenced by its `storeFile` entry (currently `signing/amaze-custom.jks`)

No GitHub Actions secrets are required for APK signing. Keep this repository private and avoid publishing or sharing the signing files.
