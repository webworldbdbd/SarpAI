# SARP AI v0.1.0 — honest status

**Not compiled or run yet.** The code was written in an environment with no network, no Android SDK and no Gradle,
so neither the build nor the unit tests have been executed. The first GitHub Actions run is the first real test;
expect to fix a few compile errors.

## Implemented (in source)
- Native Kotlin + Jetpack Compose app, minSdk 26 / target 34
- Provider adapters: Gemini, Claude, OpenAI/compatible (custom https base URL)
- API keys: EncryptedSharedPreferences (Android Keystore); never in Room, never hardcoded; errors pass through a key redactor
- Real connection test (tiny live request; shows the provider's actual result)
- Chat: persistence, history, search, rename, delete one/all, copy, share, cancel, retry/regenerate, duplicate-tap guard
- "আপনার তথ্য দিন": add/search/enable-disable/delete/clear; only top-5 keyword-relevant entries sent per request
- Notes & tasks (no reminders), light/dark/system theme, custom instructions, max tokens, temperature
- Storage screen (measured DB + cache size), clear cache, erase all local data
- Adaptive launcher icon (simplified vector); original logo kept in docs/branding
- GitHub Actions workflow: unit tests + debug APK artifact
- Unit tests: memory retrieval (incl. Bangla), key redaction

## NOT implemented yet
Streaming, Markdown/code rendering, voice in/out, attachments & Cloudinary, reminders/notifications,
device actions (intents), Gmail/Calendar, Google Drive encrypted backup/restore, export/import, app lock,
splash screen, accent colors/font size/density settings, privacy-policy text, instrumented tests.
Each needs its own step; Drive/Gmail/Calendar additionally need a Google Cloud OAuth setup.

## Known limitations
- Default model names may go stale; edit the model field in Settings.
- Release APK is unsigned (not installable) until you add signing; use the debug APK.
- Phone-stored keys can be extracted by someone with device access.


## Release readiness note
This ZIP includes a root-level GitHub Actions workflow fix and explicit cleartext-traffic blocking. These changes do not mean the project has been successfully compiled, penetration-tested, or approved for Google Play. Run the workflow and test the resulting APK before treating it as usable.

## Fixes since first review (still not compiled by the author — run the workflow)
- Bangla memory matching: tokenizer now keeps combining marks (\p{M}); added a regression test.
- Network layer: bad endpoint URL / illegal characters in an API key no longer crash the app; body-read timeouts no longer hang the spinner.
- Gemini: clearer error when the token limit is used up; connection test uses 256 tokens; default max tokens is now 2048.
- OpenAI official API: max_completion_tokens, and no custom temperature for gpt-5 / o-series models.
- Secure key storage failure (Keystore problems) no longer crashes at startup.
- Deleting the open chat while a reply is running cancels the reply; unexpected errors show a message instead of crashing.
- Chat draft text survives tab switches.
