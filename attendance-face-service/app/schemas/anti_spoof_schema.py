from typing import List
from pydantic import BaseModel


class FaceArea(BaseModel):
    x: int
    y: int
    width: int
    height: int


class AntiSpoofFace(BaseModel):
    isReal: bool
    antiSpoofScore: float
    face: FaceArea


class AntiSpoofResponse(BaseModel):
    success: bool
    faceDetected: bool
    faceCount: int
    live: bool
    faces: List[AntiSpoofFace]
    message: str