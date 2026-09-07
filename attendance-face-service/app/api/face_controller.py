import json

from fastapi import (
    APIRouter,
    UploadFile,
    File,
    Form,
    HTTPException
)

from starlette.concurrency import (
    run_in_threadpool
)

from app.services.face_service import (
    FaceService
)

from app.schemas.face_schema import (
    FaceDetectionResponse,
    FaceEmbeddingResponse,
    FaceVerificationResponse,
    AttendanceVerificationResponse
)

from app.schemas.anti_spoof_schema import (
    AntiSpoofResponse
)


router = APIRouter(
    prefix="/api/v1/face",
    tags=["Face"]
)


# =========================================================
# COMMON IMAGE VALIDATION
# =========================================================

async def read_image_file(
        file: UploadFile
) -> bytes:

    if (
        not file.content_type
        or not file.content_type.startswith(
            "image/"
        )
    ):

        raise HTTPException(
            status_code=400,
            detail="Only image files are allowed"
        )

    image_bytes = await file.read()

    if not image_bytes:

        raise HTTPException(
            status_code=400,
            detail="Image file is empty"
        )

    return image_bytes


# =========================================================
# DETECT FACE
# =========================================================

@router.post(
    "/detect",
    response_model=FaceDetectionResponse
)
async def detect_face(
        file: UploadFile = File(...)
):

    try:

        image_bytes = await read_image_file(
            file
        )

        faces = await run_in_threadpool(
            FaceService.detect_faces,
            image_bytes
        )

        return FaceDetectionResponse(

            success=True,

            faceDetected=
                len(faces) > 0,

            faceCount=
                len(faces),

            faces=
                faces,

            message=(
                "Face detected successfully"
                if faces
                else "No face detected"
            )
        )

    except HTTPException:
        raise

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# ANTI SPOOF
#
# Existing API.
# =========================================================

@router.post(
    "/anti-spoof",
    response_model=AntiSpoofResponse
)
async def anti_spoof(
        file: UploadFile = File(...)
):

    try:

        image_bytes = await read_image_file(
            file
        )

        result = await run_in_threadpool(
            FaceService.check_anti_spoofing,
            image_bytes
        )

        return AntiSpoofResponse(
            **result
        )

    except HTTPException:
        raise

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# GENERATE FACE EMBEDDING
#
# Registration API.
# =========================================================

@router.post(
    "/embedding",
    response_model=FaceEmbeddingResponse
)
async def generate_face_embedding(
        file: UploadFile = File(...)
):

    try:

        image_bytes = await read_image_file(
            file
        )

        result = await run_in_threadpool(
            FaceService.generate_embedding,
            image_bytes
        )

        return FaceEmbeddingResponse(

            success=True,

            faceDetected=True,

            faceCount=1,

            embedding=
                result["embedding"],

            embeddingSize=
                result["embeddingSize"],

            model=
                result["model"],

            message=
                "Face embedding generated successfully"
        )

    except HTTPException:
        raise

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# VERIFY TWO IMAGES
#
# Existing/testing API.
# =========================================================

@router.post(
    "/verify",
    response_model=FaceVerificationResponse
)
async def verify_faces(
        registered_file:
            UploadFile = File(...),

        current_file:
            UploadFile = File(...)
):

    try:

        registered_bytes = (
            await read_image_file(
                registered_file
            )
        )

        current_bytes = (
            await read_image_file(
                current_file
            )
        )

        result = await run_in_threadpool(
            FaceService.verify_faces,
            registered_bytes,
            current_bytes
        )

        return FaceVerificationResponse(

            success=True,

            verified=
                result["verified"],

            distance=
                result["distance"],

            threshold=
                result["threshold"],

            model=
                result["model"],

            message=(
                "Same person"
                if result["verified"]
                else "Different person"
            )
        )

    except HTTPException:
        raise

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# OLD VERIFY EMBEDDING
#
# Backward compatibility.
# =========================================================

@router.post(
    "/verify-embedding",
    response_model=FaceVerificationResponse
)
async def verify_embedding(
        registeredEmbedding:
            str = Form(...),

        current_file:
            UploadFile = File(...)
):

    try:

        current_bytes = (
            await read_image_file(
                current_file
            )
        )

        # -----------------------------------------------------
        # Parse registered embedding JSON
        # -----------------------------------------------------

        try:

            registered_embedding = (
                json.loads(
                    registeredEmbedding
                )
            )

        except (
            json.JSONDecodeError,
            TypeError
        ):

            raise HTTPException(
                status_code=400,
                detail="Invalid registered embedding"
            )

        if not isinstance(
            registered_embedding,
            list
        ):

            raise HTTPException(
                status_code=400,
                detail=
                    "Registered embedding must be a list"
            )

        if (
            len(registered_embedding)
            != FaceService.EXPECTED_EMBEDDING_SIZE
        ):

            raise HTTPException(
                status_code=400,
                detail=
                    "Invalid registered embedding size"
            )

        result = await run_in_threadpool(
            FaceService.verify_embedding,
            registered_embedding,
            current_bytes
        )

        return FaceVerificationResponse(

            success=True,

            verified=
                result["verified"],

            distance=
                result["distance"],

            threshold=
                result["threshold"],

            model=
                result["model"],

            message=(
                "Same person"
                if result["verified"]
                else "Different person"
            )
        )

    except HTTPException:
        raise

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# OPTIMIZED ATTENDANCE VERIFICATION
#
# Spring Boot calls this endpoint.
#
# ONE request:
#
# Current Image
#      ↓
# Face Detection
#      ↓
# Anti-Spoof
#      ↓
# Facenet512
#      ↓
# Compare with logged-in user's embedding
#      ↓
# Result
# =========================================================

@router.post(
    "/verify-attendance",
    response_model=AttendanceVerificationResponse
)
async def verify_attendance(
        registeredEmbedding:
            str = Form(...),

        current_file:
            UploadFile = File(...)
):

    try:

        # =====================================================
        # IMAGE
        # =====================================================

        current_bytes = (
            await read_image_file(
                current_file
            )
        )

        # =====================================================
        # REGISTERED EMBEDDING
        # =====================================================

        try:

            registered_embedding = (
                json.loads(
                    registeredEmbedding
                )
            )

        except (
            json.JSONDecodeError,
            TypeError
        ):

            raise HTTPException(
                status_code=400,
                detail=
                    "Invalid registered embedding"
            )

        if not isinstance(
            registered_embedding,
            list
        ):

            raise HTTPException(
                status_code=400,
                detail=
                    "Registered embedding must be a list"
            )

        if (
            len(registered_embedding)
            != FaceService.EXPECTED_EMBEDDING_SIZE
        ):

            raise HTTPException(
                status_code=400,
                detail=
                    "Invalid registered embedding size"
            )

        # =====================================================
        # DEEPFACE
        #
        # DeepFace is CPU/GPU heavy.
        #
        # Run it outside FastAPI event loop.
        # =====================================================

        result = await run_in_threadpool(

            FaceService.verify_attendance,

            current_bytes,

            registered_embedding
        )

        # =====================================================
        # RESPONSE
        # =====================================================

        return AttendanceVerificationResponse(

            success=
                result["success"],

            verified=
                result["verified"],

            spoof=
                result["spoof"],

            faceDetected=
                result["faceDetected"],

            faceCount=
                result["faceCount"],

            distance=
                result["distance"],

            threshold=
                result["threshold"],

            antiSpoofScore=
                result["antiSpoofScore"],

            model=
                result["model"],

            message=
                result["message"]
        )

    except HTTPException:
        raise

    except ValueError as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )

