import os
import uuid

from fastapi import Depends, FastAPI, File, Form, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from sqlalchemy.orm import Session

from database import Base, engine, get_db
from models import ScreeningRecord
from schemas import ScreeningCreate

from services.prediction import predict_image
from services.gradcam import generate_gradcam
Base.metadata.create_all(bind=engine)

app = FastAPI(title="DR Screening API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

UPLOAD_DIR = os.path.join(BASE_DIR, "uploads")
HEATMAP_DIR = os.path.join(BASE_DIR, "heatmaps")

os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(HEATMAP_DIR, exist_ok=True)

app.mount(
    "/heatmaps",
    StaticFiles(directory=HEATMAP_DIR),
    name="heatmaps"
)


@app.get("/")
def root():
    return {
        "message": "Diabetic Retinopathy Screening API is running"
    }


@app.get("/health")
def health():
    return {
        "status": "healthy"
    }

@app.post("/screenings")
def create_screening(
    screening: ScreeningCreate,
    db: Session = Depends(get_db)
):
    record = ScreeningRecord(
        patient_id=screening.patient_id,
        age=screening.age,
        gender=screening.gender,
        diabetes_duration=screening.diabetes_duration,
        image_path=screening.image_path,
        prediction=screening.prediction,
        confidence=screening.confidence,
        heatmap_path=screening.heatmap_path,
        recommendation=screening.recommendation,
        sync_status="pending"
    )

    db.add(record)
    db.commit()
    db.refresh(record)

    return {
        "message": "Screening record saved successfully",
        "id": record.id
    }

@app.get("/screenings")
def get_screenings(db: Session = Depends(get_db)):
    records = db.query(ScreeningRecord).all()

    return records

@app.post("/predict")
async def predict(
    image: UploadFile = File(...),
    patient_id: str = Form(...),
    age: int = Form(...),
    gender: str = Form(...),
    diabetes_duration: float = Form(...),
    db: Session = Depends(get_db)
):
    # -------------------------------------------------
    # 1. Create unique filename
    # -------------------------------------------------

    file_extension = os.path.splitext(image.filename)[1].lower()

    if file_extension not in [".jpg", ".jpeg", ".png"]:
        return {
            "error": "Only JPG, JPEG and PNG images are supported"
        }

    unique_id = str(uuid.uuid4())

    image_filename = f"{unique_id}{file_extension}"
    image_path = os.path.join(
        UPLOAD_DIR,
        image_filename
    )

    # -------------------------------------------------
    # 2. Save uploaded image
    # -------------------------------------------------

    contents = await image.read()

    with open(image_path, "wb") as file:
        file.write(contents)

    # -------------------------------------------------
    # 3. Run EfficientNet prediction
    # -------------------------------------------------

    result = predict_image(image_path)

    prediction = result["prediction"]
    confidence = result["confidence"]
    class_index = result["class_index"]

    # -------------------------------------------------
    # 4. Generate recommendation
    # -------------------------------------------------

    if class_index == 0:
        recommendation = (
            "No diabetic retinopathy detected. "
            "Continue routine diabetic eye screening."
        )

    elif class_index == 1:
        recommendation = (
            "Mild diabetic retinopathy detected. "
            "Routine ophthalmic follow-up is recommended."
        )

    elif class_index == 2:
        recommendation = (
            "Moderate diabetic retinopathy detected. "
            "Clinical evaluation by an ophthalmologist is recommended."
        )

    else:
        recommendation = (
            "High-risk diabetic retinopathy detected. "
            "Prompt ophthalmologist referral is recommended."
        )

    # -------------------------------------------------
    # 5. Generate Grad-CAM
    # -------------------------------------------------

    heatmap_filename = f"{unique_id}_heatmap.jpg"

    heatmap_path = os.path.join(
        HEATMAP_DIR,
        heatmap_filename
    )

    generate_gradcam(
        image_path=image_path,
        output_path=heatmap_path,
        target_class=class_index
    )

    # -------------------------------------------------
    # 6. Save screening record
    # -------------------------------------------------

    record = ScreeningRecord(
    patient_id=patient_id,
    age=age,
    gender=gender,
    diabetes_duration=diabetes_duration,
    image_path=image_path,
    prediction=prediction,
    confidence=confidence,
    heatmap_path=heatmap_path,
    recommendation=recommendation,
    sync_status="pending"
)

    db.add(record)
    db.commit()
    db.refresh(record)

    # -------------------------------------------------
    # 7. Return result to frontend
    # -------------------------------------------------

    return {
        "prediction": prediction,
        "confidence": confidence,
        "heatmap_url": f"/heatmaps/{heatmap_filename}",
        "recommendation": recommendation
    }


if __name__ == "__main__":
    # pyrefly: ignore [missing-import]
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)

@app.post("/sync")
def sync_screenings(db: Session = Depends(get_db)):

    pending_records = (
        db.query(ScreeningRecord)
        .filter(ScreeningRecord.sync_status == "pending")
        .all()
    )

    synced_count = 0

    for record in pending_records:
        record.sync_status = "synced"
        synced_count += 1

    db.commit()

    return {
        "message": "Sync completed successfully",
        "synced_count": synced_count
    }