# Signing freeze

This directory contains the fixed signing identity for the custom Amaze build.

- alias: `amaze-custom`
- keystore SHA-256: `796f92ef64d515ef76ff75f752ba2c97a9fcdad9cef39aac3faadfce4ac30fea`
- keystore.properties SHA-256: `5e00d65afd73fdd954d2c9c05ca980ee8c771a3f26d72986a5c9338fd1475a87`
- certificate SHA-256: `5F:FF:ED:B5:6F:01:67:E1:E8:B5:3E:F2:F2:1C:24:B3:9E:62:E3:9C:AE:77:68:FD:EF:96:4E:B7:57:5E:0D:E0`

Do not regenerate or replace `amaze-custom.jks` in later versions. Changing the key would break update compatibility between custom builds.

The custom key is intentionally different from the official Amaze signing key, so an official Amaze installation cannot be updated in place by this custom APK.
