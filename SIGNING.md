# Private APK signing

No keystore or signing password is stored in this direct-source branch.

Configure these **Repository Actions secrets** before running the direct build:

- `AMAZE_KEYSTORE_B64` — Base64 encoding of the existing `amaze-custom.jks`
- `AMAZE_STORE_PASSWORD`
- `AMAZE_KEY_ALIAS`
- `AMAZE_KEY_PASSWORD`

On GNU/Linux, create the Base64 value without line breaks with:

```bash
base64 -w 0 amaze-custom.jks
```

On macOS:

```bash
base64 < amaze-custom.jks | tr -d '\n'
```

Keep the repository private. The legacy branch/history contained signing material; moving signing to Actions secrets prevents new direct-source commits from continuing that practice, but it does not retroactively erase old Git objects.
