# MGN release process (Google Play)

## One-time: create the upload key

Do this ONCE on a trusted machine. The keystore and passwords must
**never** enter the git repo.

```bash
keytool -genkeypair -v \
  -keystore mgn-release.jks -alias mgn \
  -keyalg RSA -keysize 2048 -validity 10000
```

Register the key in Play Console (*Setup → App integrity → Upload key*),
then store these four repository Secrets
(*Settings → Secrets and variables → Actions*):

| Secret | Content |
|---|---|
| `MGN_KEYSTORE_B64` | `base64 -w0 mgn-release.jks` |
| `MGN_STORE_PASSWORD` | keystore password |
| `MGN_KEY_ALIAS` | key alias (`mgn`) |
| `MGN_KEY_PASSWORD` | key password |

The release workflow decodes the keystore to a temp file and exports
`MGN_KEYSTORE_PATH` / `MGN_STORE_PASSWORD` / `MGN_KEY_ALIAS` /
`MGN_KEY_PASSWORD`, which `app/build.gradle.kts` reads. Without them the
build falls back to debug keys with a warning (fine for trial artifacts,
never for Play).

## Release a version

1. Update `docs/perf.md` with fresh numbers if behavior changed.
2. Tag: `git tag v0.4.0 && git push origin v0.4.0`
   (`vMAJOR.MINOR.PATCH` → `versionName MAJOR.MINOR.PATCH`,
   `versionCode` auto-derives as `MAJOR*10000 + MINOR*100 + PATCH`).
3. CI builds signed **AAB + APK**, attaches both to the GitHub Release.
4. Play Console: Internal track → upload the AAB → Closed → Production.

## Rollback

Keep the previous AAB artifact. Roll back by promoting the older release in
Play Console; the game has no server state, saves are local-only.
