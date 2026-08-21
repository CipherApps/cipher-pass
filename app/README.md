# CipherPass - Privacy Password Manager

![License](https://img.shields.io/badge/License-MIT-yellow.svg) ![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg) ![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg) ![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-blue.svg) ![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg?style=flat&logo=kotlin)

Vault. Encrypted. Local. Nothing more.

## Why CipherPass?

- **Your phone is your vault.** Everything stays on device.
- **No accounts.** No login. No tracking who you are.
- **Small footprint.** No bloat.
- **Auditable.** Open source. Every line of code visible.
- **Privacy by design.** No internet permission. Zero telemetry hooks.

## Open Source Libraries & Tech Stack

All dependencies are FOSS and auditable:

- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** – Modern UI toolkit for Android (Material 3, Icons Extended).
- **[Navigation Compose](https://developer.android.com/jetpack/compose/navigation)** – Declarative, type-safe in-app navigation.
- **[Dagger Hilt](https://dagger.dev/hilt/)** – Dependency injection framework with Compose integration.
- **[Room Database](https://developer.android.com/training/data-storage/room)** – Local SQLite object mapping library.
- **[AndroidX Security-Crypto](https://developer.android.com/topic/security/data)** – EncryptedSharedPreferences & AES-256 GCM encryption.
- **[Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)** – Key-value data storage solution for application settings.
- **[AndroidX Biometric](https://developer.android.com/training/sign-in/biometric-auth)** – Secure hardware-backed authentication (Fingerprint / Face).
- **[AndroidX Core SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen)** – Standardized backward-compatible splash screen API.
- **[Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html)** – Asynchronous programming and reactive data streams.

## Features

- **On-Device Encryption:** Credentials are encrypted using AES-256 GCM via Android Keystore.
- **Password Generator & Strength Audit:** Built-in generator with real-time strength assessment and security reporting for weak or reused passwords.
- **Smart Organization:** Categorize entries or filter quickly using flexible `#hashtags` in your notes.
- **Biometric Authentication:** Seamless unlock using Fingerprint or Face recognition.
- **Offline-First:** No accounts, no cloud sync, no tracking. Your data stays strictly on your device.
- **Material You:** Fully customizable dynamic theme with Dark/Light mode support.

## FAQ

**Q: What if I lose my master password?**
- You can't decrypt your vault. That's the point.
- Make sure to keep an encrypted backup in a safe place.

**Q: Can you read my passwords?**
- No. The code is open-source. You can audit it.
- No cloud, no accounts, no servers.

**Q: Will you add cloud sync?**
- No. Not without changing the privacy model fundamentally.
- Recommend: sync encrypted backups via Syncthing / Nextcloud locally.

> **Enjoying CipherPass?** If you believe in local-first, encrypted, and open-source software, drop a ⭐ **Star** on this repository.

## Website
https://cipherapps.github.io/

## Support the Project

CipherPass is and will always be free and open-source.  
If you find the app useful and want to support its development, you can buy me a coffee!

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-orange.svg)](https://buymeacoffee.com/cipherapps)

## License
Distributed under the MIT License. See `LICENSE` for more information.

---

**CipherPass v1.0.0** — because your passwords are yours.  
Built with Kotlin + Jetpack Compose. No telemetry. No cloud. No BS.