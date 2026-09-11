from fastapi import FastAPI

from app.api.face_controller import router as face_router
from app.services.face_service import FaceService


app = FastAPI(
    title="College Attendance Face Service",
    description="Face recognition and anti-spoofing service",
    version="1.0.0"
)


@app.on_event("startup")
def startup_event():
    # Load the model at startup instead of on the first attendance
    # request, so the first user doesn't pay the cold-load latency.
    FaceService.warmup()


app.include_router(face_router)


@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "attendance-face-service"
    }