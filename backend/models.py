from datetime import datetime

from sqlalchemy import Column, DateTime, Float, Integer, String

from database import Base


class ScreeningRecord(Base):
    __tablename__ = "screening_records"

    id = Column(Integer, primary_key=True, index=True)

    patient_id = Column(String, index=True)

    age = Column(Integer)
    gender = Column(String)
    diabetes_duration = Column(Float)

    image_path = Column(String)

    prediction = Column(String)
    confidence = Column(Float)

    heatmap_path = Column(String)
    recommendation = Column(String)

    sync_status = Column(String, default="pending")

    created_at = Column(
        DateTime,
        default=datetime.utcnow
    )