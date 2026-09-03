from pydantic import BaseModel


class ScreeningCreate(BaseModel):
    patient_id: str
    age: int
    gender: str
    diabetes_duration: float
    image_path: str
    prediction: str
    confidence: float
    heatmap_path: str
    recommendation: str


class ScreeningResponse(BaseModel):
    prediction: str
    confidence: float
    heatmap_url: str
    recommendation: str