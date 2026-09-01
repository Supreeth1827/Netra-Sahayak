# Agent Brief — Netra Sahayak

**Paste this whole file into Claude Code, Cursor, Copilot Chat, ChatGPT or any other coding agent
as your first message.** It gives the agent everything it needs to help you set up, run, understand
and extend this project without having to explore the codebase first.

---

## Copy from here

I am working on an Android app called **Netra Sahayak** — "AI-Assisted Diabetic Retinopathy
Screening". It is a Smart India Hackathon 2026 student project. Please help me set it up, run it,
and improve it. Here is everything about it.

### What the app does

An ASHA / rural health worker opens the app, enters a patient's ID and age, captures or picks a
retinal (fundus) photo, and taps ANALYZE. The image is uploaded to a Python FastAPI backend running
an EfficientNet diabetic-retinopathy classifier. The backend returns a grade, a confidence value, a
Grad-CAM heatmap URL, and a recommendation. The app shows all of it and saves the screening locally
so it can be reviewed offline.

The single flow is: **PATIENT → IMAGE → AI SCREENING → EXPLANATION → RESULT → HISTORY**

The app deliberately uses screening language, never diagnostic language: "AI Screening Result",
"Screening Suggestion", "Referral Recommended". A disclaimer appears on the home, image-source and
result screens: *"AI screening support only. Final diagnosis must be confirmed by a qualified
healthcare professional."*

### Important context

- **Only the Android frontend exists.** The AI model and the FastAPI backend are built separately in
  Python and are NOT part of this repository. Do not try to add a model to this repo.
- **The app ships in mock mode.** A `USE_MOCK_API` build flag is `true` in debug builds, so the app
  returns a fake result and a locally-drawn placeholder heatmap. It runs end-to-end with no server
  and no internet. Set the flag to `false` to use the real backend.
- **There is no fundus camera.** Captures use the phone's ordinary rear camera through CameraX. The
  camera code sits behind an interface so a real fundus device can be added later.
- **Target user has limited technical experience.** Large buttons (68dp minimum), large text
  (18sp body, 22sp+ titles), high contrast, minimal typing, no jargon, portrait only.

### Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Retrofit · Kotlin Coroutines · Coil · Room · CameraX

AGP 8.5.2 · Kotlin 2.0.20 · Gradle 8.7 · compileSdk/targetSdk 34 · minSdk 24 ·
Compose BOM 2024.09.02 · Room 2.6.1 (KSP) · Retrofit 2.11.0 · Coil 2.7.0 · CameraX 1.3.4

Dependency versions are centralised in `gradle/libs.versions.toml` (a Gradle version catalog).
Dependency injection is a hand-written `ServiceLocator` — no Hilt or Dagger, on purpose, to keep the
project readable for students.

### Architecture

```
Composable  →  ViewModel  →  Repository  →  Retrofit  →  FastAPI
                                 ↘  Room (offline screening history)
```

Retrofit is never called from a Composable. Screens receive plain state plus callbacks; all I/O
happens inside a repository on `Dispatchers.IO`. Please preserve this separation.

### Package layout (`app/src/main/java/com/sih/netrasahayak/`)

| Path | Contents |
|---|---|
| `AppConfig.kt` | Reads `BASE_URL` and `USE_MOCK_API` from BuildConfig; timeouts; upload size limits |
| `di/ServiceLocator.kt` | **The only place** that chooses mock vs. real implementations |
| `model/` | `DrClass` (the 5 DR grades + a lenient API parser), `PatientDetails` + `PatientValidator`, `ScreeningResult`, `AppError`, `Outcome` |
| `network/` | `NetraApiService` (Retrofit), `dto/`, `RetrofitProvider`, `ConnectivityObserver` |
| `database/` | Room: `ScreeningEntity`, `ScreeningDao`, `NetraDatabase` |
| `camera/` | `RetinalCameraController` (interface), `PhoneCameraController` (CameraX), `ImageUtils` |
| `repository/` | `InferenceRepository` + Remote/Mock/Local implementations, `ScreeningRepository`, `SyncRepository` |
| `ui/theme/` | Fixed high-contrast light colour scheme, enlarged type scale |
| `ui/components/` | `PrimaryButton`, `SecondaryButton`, `ImagePreviewCard`, `ResultCard`, `ConfidenceIndicator`, `LoadingView`, `ErrorView`, `EmptyHistoryView`, `RetinalImageViewer`, `DisclaimerBanner`, `NetraScaffold` |
| `ui/navigation/` | `Routes`, `NetraNavHost` |
| `ui/{home,patient,imagesource,camera,preview,result,history,sync}/` | One package per screen |
| `ui/screening/ScreeningViewModel.kt` | Shared across the whole screening flow |

The screening flow lives in a **nested navigation graph**, and one `ScreeningViewModel` is scoped to
that graph's back-stack entry — so the patient details and the chosen image survive navigation
between screens. Leaving the graph destroys the ViewModel, so the next patient starts clean.

### The backend contract

`POST {BASE_URL}predict`, `multipart/form-data`:

| Part | Required | Notes |
|---|---|---|
| `image` | yes | JPEG, downscaled to max 1024px long edge, EXIF-rotated, quality 90 |
| `patient_id` | optional | text |
| `age` | optional | text (integer) |
| `gender` | optional | `female` / `male` / `other` |
| `diabetes_duration` | optional | text (integer years) |

Response:

```json
{
  "prediction": "Moderate Diabetic Retinopathy",
  "confidence": 0.91,
  "heatmap_url": "/results/abc123_heatmap.jpg",
  "recommendation": "Clinical evaluation by an ophthalmologist is recommended."
}
```

`prediction` parsing is forgiving — `"Moderate Diabetic Retinopathy"`, `"moderate_dr"`,
`"Moderate DR"` and `"2"` all map to the same grade. `confidence` accepts `0.91` or `91`.
`heatmap_url` may be relative (resolved against `BASE_URL`) or absolute, and may be omitted.
The five grades are No / Mild / Moderate / Severe / Proliferative Diabetic Retinopathy.

There is also a `POST /screenings` endpoint declared for syncing stored records. **No backend
implements it yet** — it is a proposed contract only.

### Setting it up

1. Install Android Studio (it bundles JDK 17 and the Android SDK).
2. Open the `NetraSahayak` folder in Android Studio.
3. If it says the SDK location is missing, accept its offer to fix it — that writes
   `local.properties`, which is intentionally not in the repository because it is machine-specific.
4. Let Gradle sync (the first run downloads Gradle 8.7 and the dependencies).
5. Pick a device or emulator running API 24 or newer, and press Run.

Command line: `./gradlew assembleDebug` (macOS/Linux) or `.\gradlew.bat assembleDebug` (Windows).

If Gradle cannot find the SDK, create `local.properties` in the project root with your own path:
`sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` on Windows, or
`sdk.dir=/home/<you>/Android/Sdk` on Linux.

### Where things are configured

`app/build.gradle.kts` — this is the only place the server URL and the mock switch live:

```kotlin
defaultConfig {
    buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")  // emulator → host machine
    buildConfigField("boolean", "USE_MOCK_API", "true")
}
buildTypes {
    release {
        buildConfigField("String", "BASE_URL", "\"https://your-server.example.com/\"")
        buildConfigField("boolean", "USE_MOCK_API", "false")
    }
}
```

`10.0.2.2` is how an Android emulator reaches the host machine. Use a LAN IP such as
`http://192.168.1.7:8000/` for a physical phone, and HTTPS in production.

Cleartext http is blocked by default at targetSdk 34, so there are two network security configs:
`app/src/debug/res/xml/` allows plain http for local development, and `app/src/main/res/xml/`
enforces HTTPS for release builds. Do not weaken the release one.

### Two things that are easy to get wrong

1. `BitmapFactory.decodeStream` **returns null by design** when `inJustDecodeBounds = true` — it only
   fills in the `Options`. Null-check the *stream*, then read the dimensions from the options.
   Getting this wrong silently breaks every image upload. See `ImageUtils.prepareForUpload`.
2. targetSdk 34 blocks cleartext http, so `http://10.0.2.2:8000/` fails without the debug network
   security config described above.

### Testing without a backend

`tools/fake_backend.py` is a stdlib-only Python stand-in that speaks the exact `/predict` contract
and serves a heatmap. Run `python tools/fake_backend.py` (listens on port 8000), set
`USE_MOCK_API = false`, rebuild, and the app talks to it over the network.

`tools/sample_fundus.jpg` is a synthetic retinal image for testing gallery selection.

### Constraints — please respect these

- Keep the MVP focused. Do **not** add a chatbot, authentication, payments, dashboards, a hospital
  management system, or heavy animations.
- Do not call Retrofit from a Composable.
- Do not put fake prediction logic anywhere except `MockInferenceRepository`.
- Never present output as a medical diagnosis.
- Do not hardcode API keys, credentials or server URLs outside `app/build.gradle.kts`.
- Keep buttons large and text readable — the user may be working outdoors on a low-end phone.

### Known gaps you could help with

- `LocalInferenceRepository` is an empty slot for on-device TensorFlow Lite inference, so screening
  could work fully offline. The file documents the exact steps.
- Sync uploads records but no server implements `POST /screenings` yet.
- Heatmaps served from the backend are referenced by URL, not cached, so a history item opened
  offline shows only the original image.
- There are no unit or instrumentation tests yet. `DrClass.fromApi` and `PatientValidator` are pure
  functions and would be the natural place to start.
- No string resources / localisation — UI text is inline English. Hindi and regional languages would
  matter a lot for the actual users.

### What I want help with

<!-- Replace this line with your actual question, for example:
     "Walk me through getting this running on my machine."
     "Add unit tests for DrClass.fromApi and PatientValidator."
     "Add Hindi translations using string resources."
     "Implement LocalInferenceRepository with a TFLite model." -->

## Copy to here
