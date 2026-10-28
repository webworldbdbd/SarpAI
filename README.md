# SARP AI — Your Personal AI Assistant

Native Android app built with Kotlin and Jetpack Compose. The app does not require a SARP AI account or cloud database in this first testing release. AI providers use the API key you enter on your own device; provider usage may cost money under that provider's terms.

## Current scope
- AI chat with Gemini, Claude, and OpenAI-compatible provider support
- Encrypted local storage for provider API keys using Android Keystore-backed AndroidX Security Crypto
- Local chat history, editable personal memory, notes/tasks, settings, and local storage management
- Bangla-friendly memory retrieval tests
- No app subscription, paywall, billing, or mandatory account in this release
- No analytics or cloud backup configured

Some planned features are **not yet implemented** (voice conversation, attachments, reminders, phone actions, Google Drive backup, Gmail/Calendar integration, app lock, and import/export). Do not assume a feature is complete unless it is present and verified in the installed app.

## Build a test APK from GitHub on Android
1. Download this ZIP and extract it. The files inside (including `settings.gradle.kts`, `app/`, and `.github/`) must be uploaded directly to the repository root. Do **not** upload only the ZIP file and do not put the project inside an extra `SarpAI/` folder.
2. In the GitHub repository, open **Actions** → **SARP AI Android Build** → **Run workflow**. A push to `main` or `master` also starts the workflow.
3. Open the completed run → **Artifacts** → download `SARP-AI-debug-apk`, extract the downloaded artifact, then install `app-debug.apk` on your own phone.
4. If the run fails, open the first failed step and copy the full error log. The workflow has not been verified by a successful remote build yet.

## Add your own AI key
Open Settings, choose a provider, enter your own key, save, and use Test connection. Never share the key or commit it to GitHub. The app's local encryption reduces exposure but cannot protect a key from every attack, especially on a compromised/unlocked device.

## Security and Play Store status
This is a **testing build**, not yet a Play Store-ready release. Before publishing, test on real devices, resolve all build/runtime issues, review privacy disclosures and provider data flows, verify current Google Play target-API and Data safety requirements, add a properly protected release signing process, and run security/privacy checks. No app can honestly be guaranteed “unhackable”; this project uses defensive settings but still requires testing and maintenance.
