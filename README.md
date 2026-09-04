# Netra Sahayak — Explainable AI for Diabetic Retinopathy Screening

**SIH26038 — Explainable AI for Diabetic Retinopathy Screening in Rural India**

> AI screening support only. Final diagnosis must be confirmed by a qualified healthcare professional.

Netra Sahayak is an AI-assisted diabetic retinopathy screening system designed for ASHA workers and rural healthcare workflows.

The project combines:

- 📱 Android application for patient and screening workflow
- 🧠 EfficientNet-B3 diabetic retinopathy classification
- 🔥 Grad-CAM explainability
- ⚡ FastAPI backend
- 💾 SQLite database
- 📦 Offline-first Room storage
- 🔄 Automatic synchronization when connectivity returns

The system is designed to continue working when internet connectivity is unavailable and synchronize pending screening data when the connection is restored.

---

## 1. System Overview

```text
                    NETRA SAHAYAK
                         │
                         ▼
              ┌─────────────────────┐
              │   Android App       │
              │   Jetpack Compose   │
              └──────────┬──────────┘
                         │
                  Patient + Image
                         │
                         ▼
              ┌─────────────────────┐
              │     FastAPI         │
              │      Backend        │
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │   EfficientNet-B3   │
              │   DR Classification │
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │      Grad-CAM       │
              │   Explainability    │
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │ SQLite + Heatmaps   │
              │ Screening Records   │
              └──────────┬──────────┘
                         │
                         ▼
                  Result to Android
```

The Android application handles the user workflow and local storage.

The Python backend performs the actual AI inference and Grad-CAM generation.

---

## 2. Repository Structure

```text
SIH26038-DR-Screening/
│
├── app/                              # Android application
│   └── src/main/java/com/sih/netrasahayak/
│       ├── camera/
│       ├── database/
│       ├── di/
│       ├── model/
│       ├── network/
│       ├── repository/
│       └── ui/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
│
├── backend/                          # FastAPI + AI backend
│   ├── main.py                       # FastAPI application
│   ├── database.py                   # SQLAlchemy database setup
│   ├── models.py                     # Screening database model
│   ├── schemas.py                    # API schemas
│   ├── test_model.py                 # Model test
│   ├── test_gradcam.py               # Grad-CAM test
│   │
│   ├── model/
│   │   └── efficientnet_b3_300_best.pth
│   │                                  # Trained EfficientNet-B3 model
│   │
│   └── services/
│       ├── prediction.py              # EfficientNet inference
│       └── gradcam.py                 # Grad-CAM generation
│
└── README.md
```

Runtime-generated files such as the SQLite database, uploaded images, generated heatmaps and Python cache files are intentionally excluded from Git.

---

## 3. Technology Stack

### Android

- Kotlin
- Jetpack Compose
- MVVM architecture
- Retrofit
- Room Database
- CameraX
- Coil
- Coroutines

### Backend

- Python
- FastAPI
- SQLAlchemy
- SQLite
- PyTorch
- Torchvision
- timm
- Pillow
- OpenCV
- pytorch-grad-cam

### AI

- EfficientNet-B3
- Five-class diabetic retinopathy classification
- Grad-CAM explainability

---

## 4. Diabetic Retinopathy Classes

The AI model supports five classes:

```text
0 → No DR
1 → Mild
2 → Moderate
3 → Severe
4 → Proliferative DR
```

The model returns the predicted class together with a confidence score.

---

## 5. Android Application Flow

The Android application follows this workflow:

```text
HOME
  ↓
START NEW SCREENING
  ↓
PATIENT DETAILS
  ↓
RETINAL IMAGE
  ├── TAKE PHOTO
  └── CHOOSE FROM GALLERY
  ↓
IMAGE PREVIEW
  ↓
ANALYZE IMAGE
  ↓
AI RESULT
  ├── Prediction
  ├── Confidence
  ├── Recommendation
  └── Grad-CAM explanation
  ↓
SCREENING HISTORY
```

The application stores screening information locally using Room.

---

## 6. Offline-First Workflow

Offline operation is an important part of the system.

When there is no internet connection:

```text
Patient Details
      ↓
Retinal Image
      ↓
Local Storage
      ↓
Pending Analysis
      ↓
Screening History
```

The user can continue using the application without being blocked by the network.

When connectivity returns:

```text
Internet Restored
       ↓
SYNC DATA
       ↓
Pending Screening
       ↓
FastAPI /predict
       ↓
EfficientNet-B3
       ↓
Grad-CAM
       ↓
AI Result
       ↓
Room Record Updated
       ↓
Marked as Synced
```

This allows screening records to survive temporary connectivity problems common in rural environments.

---

## 7. Local Database

The Android application uses Room to store screening records locally.

The local screening record contains information such as:

- Patient ID
- Age
- Gender
- Diabetes duration
- Original image path
- Prediction
- Confidence
- Recommendation
- Heatmap URL
- Creation time
- Synchronization status

The backend separately uses SQLite through SQLAlchemy for server-side screening records.

---

## 8. FastAPI Backend

The backend is located in:

```text
backend/
```

The main FastAPI application is:

```text
backend/main.py
```

Start the backend from the `backend` directory:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

The API can then be accessed at:

```text
http://localhost:8000
```

For a physical Android phone, the phone and computer must normally be connected to the same network and the Android application must use the computer's LAN IP.

---

## 9. Backend API

### Health Check

```http
GET /health
```

Example response:

```json
{
  "status": "healthy"
}
```

---

### AI Screening

```http
POST /predict
```

Content type:

```text
multipart/form-data
```

Fields:

| Field | Type | Description |
|---|---|---|
| `image` | file | Retinal image |
| `patient_id` | text | Patient identifier |
| `age` | integer | Patient age |
| `gender` | text | Gender |
| `diabetes_duration` | number | Diabetes duration |

The backend:

1. Receives the retinal image
2. Saves the uploaded image
3. Preprocesses the image
4. Runs EfficientNet-B3
5. Determines the predicted DR class
6. Calculates confidence
7. Generates a Grad-CAM heatmap
8. Saves the screening record
9. Returns the result to Android

Example response:

```json
{
  "prediction": "Proliferative DR",
  "confidence": 0.9998,
  "heatmap_url": "/heatmaps/example_heatmap.jpg",
  "recommendation": "Clinical evaluation by an ophthalmologist is recommended."
}
```

---

### Screening Record Upload

```http
POST /screenings
```

This endpoint is used by the synchronization workflow to upload completed local screening records to the backend.

---

### Screening History

```http
GET /screenings
```

Returns screening records stored by the backend.

---

### Synchronization Endpoint

```http
POST /sync
```

The project also contains a synchronization endpoint for backend integration and demonstration.

The Android application primarily performs synchronization through the screening upload flow implemented in `SyncRepository` and `SyncDataSource`.

---

## 10. Grad-CAM Explainability

Grad-CAM is generated on the Python backend.

It is **not calculated on Android**.

The process is:

```text
Retinal Image
      ↓
EfficientNet-B3
      ↓
Predicted Class
      ↓
Grad-CAM
      ↓
Heatmap
      ↓
HTTP /heatmaps/...
      ↓
Android App
```

The Android application provides three viewing modes:

```text
[ ORIGINAL ] [ HEATMAP ] [ OVERLAY ]
```

### ORIGINAL

Displays the original retinal image.

### HEATMAP

Displays the Grad-CAM explanation generated by the backend.

### OVERLAY

Displays the heatmap over the original retinal image.

The highlighted regions indicate areas that contributed to the model's prediction.

Grad-CAM is an explanation of the model's behaviour and should not be interpreted as a clinical diagnosis.

---

## 11. AI Model

The trained model is included in this repository:

```text
backend/model/efficientnet_b3_300_best.pth
```

Model:

```text
EfficientNet-B3
```

Input size:

```text
300 × 300
```

Number of classes:

```text
5
```

The backend loads the model using PyTorch/timm and performs inference on the available device.

The project can run on CPU when CUDA is unavailable.

---

## 12. Backend Model Testing

The repository includes:

```text
backend/test_model.py
backend/test_gradcam.py
```

The model test verifies that the EfficientNet-B3 model can be loaded and used for prediction.

The Grad-CAM test verifies that a heatmap can be generated successfully from an input retinal image.

---

## 13. Android Configuration

The backend URL is configured through `BuildConfig`.

In:

```text
app/build.gradle.kts
```

the application can be configured with:

```kotlin
buildConfigField(
    "String",
    "BASE_URL",
    "\"http://YOUR_COMPUTER_IP:8000/\""
)

buildConfigField(
    "boolean",
    "USE_MOCK_API",
    "false"
)
```

### Emulator

When FastAPI is running on the same computer:

```text
http://10.0.2.2:8000/
```

### Physical Android phone

Use the computer's LAN IP:

```text
http://192.168.x.x:8000/
```

The phone and computer must be reachable on the same network.

Do not copy a specific developer's LAN IP into another machine. Replace it with the current computer's IP address.

---

## 14. Running the Android Application

### Requirements

- Android Studio
- Android SDK
- JDK
- Android device or emulator

Open the repository in Android Studio:

```text
SIH26038-DR-Screening/
```

Then allow Gradle to synchronize.

Build from Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Install on a connected device:

```powershell
.\gradlew.bat installDebug
```

---

## 15. Running the Backend

Open a terminal:

```powershell
cd C:\SIH\SIH26038-DR-Screening\backend
```

Activate the Python virtual environment if one is being used:

```powershell
cd ..
.\.venv\Scripts\Activate.ps1
cd backend
```

Start FastAPI:

```powershell
uvicorn main:app --host 0.0.0.0 --port 8000
```

Verify:

```text
http://localhost:8000/health
```

Expected:

```json
{
  "status": "healthy"
}
```

---

## 16. End-to-End Architecture

The complete working path is:

```text
ANDROID
   │
   │ Patient details + retinal image
   ▼
FastAPI
   │
   ▼
EfficientNet-B3
   │
   ├──────────────► Prediction
   │
   ▼
Grad-CAM
   │
   └──────────────► Heatmap
                     │
                     ▼
                  FastAPI
                     │
                     ▼
                  Android
                     │
             ┌───────┴────────┐
             ▼                ▼
          Result           Room DB
                              │
                              ▼
                         History / Sync
```

---

## 17. Offline → Online Synchronization

A tested synchronization scenario is:

```text
1. Android phone loses internet
2. User creates a screening
3. Screening is stored locally
4. History displays the pending screening
5. Internet connection is restored
6. User opens Sync Data
7. Pending screening is sent to the backend
8. FastAPI performs EfficientNet inference
9. Grad-CAM heatmap is generated
10. Result is returned
11. Local Room record is updated
12. Record is marked synced
13. Pending count becomes zero
```

This allows the application to tolerate temporary network interruptions.

---

## 18. Data and Runtime Files

The following runtime-generated files are intentionally not stored in Git:

```text
backend/screening.db
backend/uploads/
backend/heatmaps/
__pycache__/
*.pyc
```

These files are created locally when the application runs.

The trained EfficientNet model is included because it is required for reproducing the backend inference workflow.

---

## 19. Security and Privacy Notes

This project is an MVP/prototype for SIH.

Current limitations include:

- No user authentication
- No production authorization system
- Local HTTP may be used during development
- Production deployment should use HTTPS
- Patient data should be handled according to applicable privacy and healthcare requirements
- The AI output is screening assistance, not a final diagnosis

---

## 20. Clinical Disclaimer

Netra Sahayak is intended as an AI-assisted screening support tool.

It does **not** replace:

- Ophthalmologist examination
- Clinical diagnosis
- Professional medical judgement
- Appropriate retinal imaging equipment

Final diagnosis and treatment decisions must be made by qualified healthcare professionals.

---

## 21. Current MVP Limitations

1. A dedicated fundus camera is not included.
2. Screening quality depends on the quality of the retinal image.
3. The AI model is intended for screening support and requires appropriate clinical validation before real-world clinical deployment.
4. TFLite-based fully offline AI inference on the Android device is not currently implemented.
5. Server-generated heatmaps require connectivity to the backend unless separately cached.
6. Authentication is outside the current MVP scope.
7. The backend is currently intended to run locally for demonstration and development.

---

## 22. Project Status

### Implemented

- [x] Android Jetpack Compose application
- [x] Patient details workflow
- [x] Camera input
- [x] Gallery input
- [x] Image preview
- [x] FastAPI integration
- [x] EfficientNet-B3 inference
- [x] Five-class DR prediction
- [x] Confidence score
- [x] Grad-CAM generation
- [x] Heatmap display
- [x] Original / Heatmap / Overlay views
- [x] Room local database
- [x] Offline screening storage
- [x] Screening history
- [x] Pending screening state
- [x] Reconnection synchronization
- [x] Backend SQLite storage
- [x] Screening upload endpoint
- [x] Backend health check
- [x] Backend model test
- [x] Backend Grad-CAM test

---

## 23. Team Project

**Smart India Hackathon 2026**

Problem Statement:

```text
SIH26038
Explainable AI for Diabetic Retinopathy Screening in Rural India
```

Project:

```text
Netra Sahayak
```

The system is designed around explainable, accessible and connectivity-tolerant AI-assisted diabetic retinopathy screening.

---

## 24. License / Usage

This repository is an academic prototype developed for Smart India Hackathon 2026.

The AI model and software should not be considered clinically validated or approved for autonomous medical diagnosis.