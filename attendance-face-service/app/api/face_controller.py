from fastapi import (
    APIRouter,
    UploadFile,
    File,
    Form,
    HTTPException
)

from app.services.face_service import FaceService

from app.schemas.face_schema import (
    FaceDetectionResponse,
    FaceEmbeddingResponse,
    FaceVerificationResponse,
    FaceEmbeddingVerificationRequest
)

from app.schemas.anti_spoof_schema import (
    AntiSpoofResponse
)


router = APIRouter(
    prefix="/api/v1/face",
    tags=["Face"]
)


# =========================================================
# FACE DETECTION
# =========================================================

@router.post(
    "/detect",
    response_model=FaceDetectionResponse
)
async def detect_face(
    file: UploadFile = File(...)
):

    try:

        if (
            not file.content_type
            or not file.content_type.startswith("image/")
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

        faces = FaceService.detect_faces(
            image_bytes
        )

        return FaceDetectionResponse(
            success=True,
            faceDetected=len(faces) > 0,
            faceCount=len(faces),
            faces=faces,
            message=(
                "Face detected successfully"
                if faces
                else "No face detected"
            )
        )

    except HTTPException:
        raise

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# ANTI SPOOFING
# =========================================================

@router.post(
    "/anti-spoof",
    response_model=AntiSpoofResponse
)
async def anti_spoof(
    file: UploadFile = File(...)
):

    try:

        if (
            not file.content_type
            or not file.content_type.startswith("image/")
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

        result = FaceService.check_anti_spoofing(
            image_bytes
        )

        return AntiSpoofResponse(**result)

    except HTTPException:
        raise

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# =========================================================
# FACE EMBEDDING
# =========================================================

@router.post(
    "/embedding",
    response_model=FaceEmbeddingResponse
)
async def generate_face_embedding(
    file: UploadFile = File(...)
):

    try:

        if (
            not file.content_type
            or not file.content_type.startswith("image/")
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

        result = FaceService.generate_embedding(
            image_bytes
        )

        return FaceEmbeddingResponse(
            success=True,
            faceDetected=True,
            faceCount=1,
            embedding=result["embedding"],
            embeddingSize=result["embeddingSize"],
            model=result["model"],
            message="Face embedding generated successfully"
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


@router.post(
    "/verify",
    response_model=FaceVerificationResponse
)
async def verify_faces(
    registered_file: UploadFile = File(...),
    current_file: UploadFile = File(...)
):

    try:

        # Validate first image
        if (
            not registered_file.content_type
            or not registered_file.content_type.startswith("image/")
        ):
            raise HTTPException(
                status_code=400,
                detail="Registered file must be an image"
            )

        # Validate second image
        if (
            not current_file.content_type
            or not current_file.content_type.startswith("image/")
        ):
            raise HTTPException(
                status_code=400,
                detail="Current file must be an image"
            )

        # Read files
        registered_bytes = await registered_file.read()
        current_bytes = await current_file.read()

        if not registered_bytes:
            raise HTTPException(
                status_code=400,
                detail="Registered image is empty"
            )

        if not current_bytes:
            raise HTTPException(
                status_code=400,
                detail="Current image is empty"
            )

        # Verify
        result = FaceService.verify_faces(
            registered_bytes,
            current_bytes
        )

        return FaceVerificationResponse(
            success=True,
            verified=result["verified"],
            distance=result["distance"],
            threshold=result["threshold"],
            model=result["model"],
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
# VERIFY CURRENT IMAGE AGAINST REGISTERED EMBEDDING
# =========================================================

@router.post(
    "/verify-embedding",
    response_model=FaceVerificationResponse
)
async def verify_embedding(
    registeredEmbedding: str = Form(...),
    current_file: UploadFile = File(...)
):

    try:

        if (
            not current_file.content_type
            or not current_file.content_type.startswith("image/")
        ):
            raise HTTPException(
                status_code=400,
                detail="Current file must be an image"
            )

        current_bytes = await current_file.read()

        if not current_bytes:
            raise HTTPException(
                status_code=400,
                detail="Current image is empty"
            )

        # JSON string → List
        import json

        registered_embedding = json.loads(
            registeredEmbedding
        )

        if not registered_embedding:

            raise HTTPException(
                status_code=400,
                detail="Registered embedding is required"
            )

        if len(registered_embedding) != 512:

            raise HTTPException(
                status_code=400,
                detail="Invalid registered embedding size"
            )

        result = FaceService.verify_embedding(
            registered_embedding,
            current_bytes
        )

        return FaceVerificationResponse(
            success=True,
            verified=result["verified"],
            distance=result["distance"],
            threshold=result["threshold"],
            model=result["model"],
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