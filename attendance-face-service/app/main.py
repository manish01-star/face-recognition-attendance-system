from fastapi import FastAPI

from app.api.face_controller import router as face_router


app = FastAPI(
    title="College Attendance Face Service",
    description="Face recognition and anti-spoofing service",
    version="1.0.0"
)


app.include_router(face_router)


@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "attendance-face-service"
    }