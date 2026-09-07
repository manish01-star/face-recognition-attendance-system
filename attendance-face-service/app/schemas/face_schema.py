from typing import List, Optional
from pydantic import BaseModel


class FaceArea(BaseModel):
    x: int
    y: int
    width: int
    height: int


class FaceDetectionResponse(BaseModel):
    success: bool
    faceDetected: bool
    faceCount: int
    faces: List[FaceArea]
    message: str


class FaceEmbeddingResponse(BaseModel):
    success: bool
    faceDetected: bool
    faceCount: int
    embedding: Optional[List[float]] = None
    embeddingSize: int = 0
    model: str
    message: str


class FaceVerificationResponse(BaseModel):
    success: bool
    verified: bool
    distance: float
    threshold: float
    model: str
    message: str


class FaceEmbeddingVerificationRequest(BaseModel):
    registeredEmbedding: List[float]


# =========================================================
# NEW
# Attendance verification response
# =========================================================

class AttendanceVerificationResponse(BaseModel):
    success: bool

    verified: bool

    spoof: bool

    faceDetected: bool

    faceCount: int

    distance: float

    threshold: float

    antiSpoofScore: float

    model: str

    message: str