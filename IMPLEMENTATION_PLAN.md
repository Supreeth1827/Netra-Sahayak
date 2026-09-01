# Implementation Plan — Netra Sahayak

The design decisions behind the Android frontend, and the order the work was carried out in.
Kept in the repository so the reasoning is available to anyone picking the project up.

---

## Context

Smart India Hackathon 2026 project: an AI-assisted diabetic-retinopathy screening app for ASHA and
rural health workers in India.

The Android frontend had to be built **before** the AI model, and had to connect to a
FastAPI + EfficientNet + Grad-CAM backend later without being rearchitected. There was no existing
Android project, so the whole app is new.

**Goal:** a buildable, navigable Compose app covering
`PATIENT → IMAGE → AI SCREENING → EXPLANATION → RESULT → HISTORY`, running today against a mock
repository and switchable to the real backend with a single flag.

---

## Core constraints

The intended user is a health worker who may have limited experience with smartphones, often working
outdoors, on a low-end device, with unreliable connectivity. That drove every UI decision:

- Buttons at least 68dp tall, full width
- Body text 18sp, titles 22sp and above
- One fixed high-contrast light theme — no dark mode, no dynamic colour, so the app looks identical
  on every phone and stays readable in daylight
- Portrait only
- As little typing as possible (gender is chosen by tapping, two short text fields in total)
- Screening language throughout, never diagnostic language

---

## Architecture

```
Composable  →  ViewModel  →  Repository  →  Retrofit  →  FastAPI
                                 ↘  Room (offline screening history)
```

Retrofit is never called from a Composable. Screens receive plain state and callbacks; all I/O runs
in a repository on `Dispatchers.IO`.

Dependency injection is a hand-written `ServiceLocator` rather than Hilt or Dagger — deliberately,
so the wiring stays readable for a student project and there is no annotation-processing magic to
explain.

**Versions:** AGP 8.5.2 · Kotlin 2.0.20 · Gradle 8.7 · compileSdk/targetSdk 34 · minSdk 24 ·
Compose BOM 2024.09.02 · Room 2.6.1 (KSP) · Retrofit 2.11.0 · Coil 2.7.0 · CameraX 1.3.4.
All centralised in `gradle/libs.versions.toml`.

---

## Key design decisions

### One ViewModel for the screening flow

The screening screens live in a **nested navigation graph**, and a single `ScreeningViewModel` is
scoped to that graph's back-stack entry. Patient details and the selected image therefore survive
navigation between screens without being squeezed into route arguments.

Leaving the graph destroys the ViewModel, which means the next patient automatically starts from a
clean form — no manual reset needed.

### Swappable inference

`InferenceRepository` is an interface with three implementations:

| Implementation | Purpose |
|---|---|
| `RemoteInferenceRepository` | Production — `POST /predict` to the FastAPI backend |
| `MockInferenceRepository` | Development — fake result and a locally-drawn placeholder heatmap |
| `LocalInferenceRepository` | Reserved for on-device TensorFlow Lite inference (not implemented) |

`ServiceLocator` is the only place that chooses between them, driven by the `USE_MOCK_API`
BuildConfig flag. The release build type forces it to `false`, so a release APK can never serve
mock data. No fake prediction logic exists anywhere in the production path.

### Swappable camera

The project has no dedicated fundus camera, so capture uses the phone's ordinary rear camera via
CameraX. All of it sits behind a `RetinalCameraController` interface; adding a real fundus device
later means writing a second implementation and returning it from `ServiceLocator`, with no changes
to any screen.

### Offline-first

Patient entry, camera, gallery selection and the entire screening history work with no internet.
Only the analysis request itself needs a connection, and the app says so plainly rather than
blocking navigation.

Results are written to Room immediately after the server replies, marked `synced = false`. The Sync
screen reports connectivity and the pending count.

### Explainable AI display

Grad-CAM is computed in Python; Android only displays what the API returns in `heatmap_url`.
`RetinalImageViewer` offers ORIGINAL / HEATMAP / OVERLAY (the heatmap drawn over the original at
55% alpha). When no heatmap is supplied the last two modes are disabled with a short explanation.

### Errors

Every failure the user can see is an `AppError` with a plain-language message. Raw exceptions and
stack traces are never surfaced. Covered: camera permission denied, camera unavailable, no image
selected, invalid image, image too large, no internet, timeout, server unreachable, HTTP error
codes, unparseable response, failed upload, local storage failure, empty Patient ID, invalid age,
offline model unavailable.

Duplicate submissions are guarded in `ScreeningViewModel.analyze()` and `SyncViewModel.syncNow()`.

---

## Build order

1. **Gradle scaffolding** — version catalog, wrapper, `BASE_URL` and `USE_MOCK_API` as BuildConfig
   fields, manifest, theme and resources.
2. **Domain model** — `DrClass` (the five grades plus a forgiving API parser), `PatientDetails`
   with `PatientValidator`, `ScreeningResult`, `AppError`, `Outcome`.
3. **Data layer** — Retrofit service and DTOs, `RetrofitProvider`, `ConnectivityObserver`, the Room
   entity/DAO/database.
4. **Camera and image handling** — `RetinalCameraController` + CameraX implementation, and
   `ImageUtils` for downsampling, EXIF rotation, compression and app-private storage of originals.
5. **Repositories** — inference (remote / mock / local), screening history, sync.
6. **Theme and reusable components** — buttons, cards, confidence indicator, state views, the
   Grad-CAM viewer, the disclaimer banner, the shared scaffold.
7. **ViewModels and navigation** — the shared screening ViewModel, routes, the nav host.
8. **Screens** — home, patient details, image source, camera, preview, result, history list and
   detail, sync.
9. **Build, run and fix** — both variants compiled, then the whole app exercised on a device.

---

## Verification performed

Both `assembleDebug` and `assembleRelease` build cleanly. The app was then run end to end on an
emulator:

- Home → patient → gallery → preview → analyze → result
- Form validation with empty and invalid fields
- Camera permission denied (readable message, no crash) and granted (capture → preview)
- ORIGINAL / HEATMAP / OVERLAY toggle
- History list and detail, with the stored original image reloading from app storage
- Sync: status, pending count, upload, count returning to zero
- Airplane mode: everything except analysis still usable
- The real Retrofit path against a stand-in server speaking the `/predict` contract — the multipart
  request arrived with `image`, `patient_id`, `age`, `gender` and `diabetes_duration`, the response
  rendered, and the heatmap was fetched from `/results/`
- Server down and no-internet error paths, with no crashes in logcat

### Two defects this caught

1. **`BitmapFactory.decodeStream` returns `null` by design when `inJustDecodeBounds = true`** — it
   only populates the `Options`. An `?: return failure` on that call therefore fired for every
   image, which silently disabled the mock heatmap and would have made *every real upload* fail as
   "not a valid image". The stream must be null-checked separately and the dimensions read from the
   options afterwards.
2. **targetSdk 34 blocks cleartext HTTP**, so the documented `http://10.0.2.2:8000/` development URL
   was rejected by OkHttp. Fixed with a network security config: permissive in `src/debug/`,
   HTTPS-only in `src/main/` for release builds.

---

## Deliberately out of scope

No chatbot, no authentication, no payments, no dashboards, no hospital management, no doctor social
network, no heavy animations. The MVP stays on the single screening flow.

---

## Known limitations

1. **No fundus camera.** Ordinary phone photos are not clinically adequate input for a DR model. The
   camera abstraction is ready for a real device, but screening quality depends on having one.
2. **Mock results carry no clinical meaning.** The grade is derived from the Patient ID hash so
   demos are repeatable.
3. **`POST /screenings` (sync) is a proposed contract.** No backend implements it yet.
4. **TFLite offline inference is an integration point only**, not implemented.
5. **Gallery URI lifetime.** A picked `content://` URI is valid for the current process; if the app
   is killed while the preview is open, the image must be re-picked. Once analysed it is copied into
   app storage permanently.
6. **Server-hosted heatmaps are not cached.** Only the URL is stored, so a history item opened
   offline shows the original image alone. Mock heatmaps are local files and always available.
7. **Portrait and light theme only** — deliberate, for consistency and daylight readability.
8. **No authentication**, by design for the MVP.
9. **No automated tests yet.** `DrClass.fromApi` and `PatientValidator` are pure functions and are
   the natural starting point.
10. **No string resources / localisation.** UI text is inline English; Hindi and regional languages
    would matter a great deal for the actual users.
