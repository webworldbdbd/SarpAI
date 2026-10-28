# Security notes

- Do not hardcode provider API keys or commit secrets to this repository.
- Provider keys are stored locally using AndroidX Security Crypto and Android Keystore-backed key material. This is defense-in-depth, not a guarantee against a compromised device.
- Android backup is disabled for the app, and cleartext HTTP traffic is explicitly disabled.
- Chat, notes, and memory are stored locally in the app database in this version. AI prompts/messages sent to a selected AI provider leave the device as required to answer; review that provider's privacy and retention terms.
- Report a security issue privately to the repository maintainer rather than posting exploit details publicly.
- Before Play Store release, review dependencies, exported components, network behavior, privacy policy, Data safety declarations, signing-key protection, and current Play policy requirements.
