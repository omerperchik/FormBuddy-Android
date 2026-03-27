# FormBuddy Android

AI-powered form filling app for Android. The companion to [FormBuddy iOS](https://github.com/omerperchik/FormBuddy-iOS).

## Features

- **Scan Documents** — Use your camera to scan paper forms
- **Upload PDFs** — Import PDF files or photos from your device
- **Forms Library** — Browse a searchable library of common forms
- **Chat Mode** — Fill forms conversationally, one field at a time
- **Voice Mode** (Pro) — Talk to fill forms with voice input/output
- **Agent Mode** (Pro) — Auto-fill forms from your profile data
- **Smart Field Detection** — OCR + ML Kit + Gemini Nano for accurate field recognition
- **Profile Management** — Personal, family, and business profiles
- **Signature Drawing** — Draw and save signatures
- **Encrypted Storage** — SQLCipher encrypted database + Jetpack Security
- **Biometric Lock** (Pro) — Protect profiles with fingerprint/face
- **12 Languages** — EN, HE, ES, ZH, FR, DE, PT-PT, PT-BR, AR, RU, JA, HI
- **PDF Export** — Share filled forms as PDF

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** + **SQLCipher** (encrypted database)
- **Jetpack Security** (EncryptedSharedPreferences, EncryptedFile)
- **Hilt** (dependency injection)
- **CameraX** (document scanning)
- **ML Kit** (OCR text recognition)
- **Gemini Nano** (on-device AI field refinement)
- **Firebase** (Auth, Firestore, Functions, Storage, Remote Config)
- **Google Play Billing** (subscriptions)
- **PDFBox Android** (PDF parsing)
- **Coil** (image loading)
- **Lottie** (animations)

## Setup

1. Clone the repository
2. Add your `google-services.json` to `app/`
3. Open in Android Studio
4. Build and run

## Architecture

```
app/src/main/java/com/formbuddy/android/
├── data/
│   ├── local/          # Room DB, SQLCipher, EncryptedPrefs
│   ├── remote/         # Firebase services
│   ├── model/          # Data models
│   └── repository/     # Repository pattern
├── domain/
│   ├── analysis/       # PDF parsing, OCR, field inference, Gemini
│   ├── filling/        # Conversation manager, response classifier
│   ├── tts/            # Text-to-speech (cloud + device)
│   └── speech/         # Speech recognition
└── ui/
    ├── theme/          # Material 3 theme
    ├── navigation/     # Navigation graph
    ├── screens/        # All screens with ViewModels
    └── components/     # Reusable Compose components
```

## Security

- Database encrypted with **SQLCipher** (AES-256)
- Preferences encrypted with **EncryptedSharedPreferences**
- Document files encrypted with **EncryptedFile** (AES-256-GCM)
- Encryption keys managed via **Android Keystore**
- Optional **biometric authentication** for profile access
- **No cleartext traffic** allowed
- Sensitive data **redacted** before on-device AI processing
