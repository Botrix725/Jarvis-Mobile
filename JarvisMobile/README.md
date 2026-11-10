# JARVIS Mobile — BOTRIX STUDIOS

A native Android starter project (Kotlin + Jetpack Compose) for a personal AI assistant.

## Current starter features
- First-run provider/key setup for Google Gemini or OpenAI
- Encrypted local storage for the API key (AndroidX Security Crypto)
- Gemini `generateContent` and OpenAI Chat Completions API calls
- Chat interface with text-to-speech responses
- Speech-to-text input using Android speech recognition
- Text/TXT and text-extractable document attachment picker (PDF extraction is not implemented yet)
- Launch YouTube, Chrome, WhatsApp and Android Settings when installed
- Basic media-volume command
- Accessibility-service foundation, which the user must explicitly enable in Android Settings

## Build the APK
Requirements: Android Studio (recommended), JDK 17, Android SDK Platform 35.

1. Open this folder in Android Studio.
2. Allow Gradle sync and install the Android SDK Platform 35 if prompted.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Android Studio will show the generated APK path, normally `app/build/outputs/apk/debug/app-debug.apk`.
5. Transfer the APK to your Android phone and install it. You may need to allow installation from that source.

Command line alternative (after installing Android SDK and Gradle):
`gradle assembleDebug`

## Important limitations / next development steps
- This is a starter project, not a finished ChatGPT-equivalent assistant.
- The build environment used to prepare this source archive did not have Android SDK or Gradle installed, so no APK binary has been compiled or device-tested here.
- PDF/DOCX/XLSX parsing, image understanding, conversation history, streaming responses, wake-word listening, robust multi-step agent planning, and secure backend proxy are not implemented yet.
- API keys are encrypted at rest on the phone, but a production app should ideally use a backend proxy and provider-specific key management. Never share your API key.
- Accessibility control is highly sensitive. Android requires explicit user opt-in. This starter only includes a narrowly scoped visible-text click helper; it does not implement arbitrary cross-app automation or message sending.
- Opening apps depends on those apps being installed and Android allowing their launch intents. Volume control affects media volume only.
- Google/OpenAI API names, model availability, pricing and quotas can change. Change the model endpoint in `AiClient` if required by your provider account.
- Do not publish this starter as a production app without privacy disclosures, permission rationale, security review, API error handling and testing across Android versions.


## Build without a capable laptop (GitHub cloud build)

This project includes `.github/workflows/build-apk.yml`.

1. Create a GitHub account and a **new private repository** in the browser.
2. Upload the extracted project files/folders to the repository (the contents of `JarvisMobile`, not the outer ZIP).
3. Open the repository's **Actions** tab and select **Build JARVIS Android APK**.
4. Press **Run workflow** (or let the first push start it).
5. Wait for the workflow to finish successfully.
6. Open the completed workflow run, scroll to **Artifacts**, and download `JARVIS-debug-apk`.
7. Extract that downloaded artifact ZIP; it contains `app-debug.apk`. Transfer it to your phone and install.

GitHub Actions performs the Android build on its cloud runner. Keep the repository private because this is your app source. Never commit real API keys.
