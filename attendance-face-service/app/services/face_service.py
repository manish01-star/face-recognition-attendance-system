import cv2
import numpy as np
from deepface import DeepFace


class FaceService:

    # =========================================================
    # FACE CONFIGURATION
    # =========================================================

    MODEL_NAME = "Facenet512"

    # ONLY detector change:
    # RetinaFace -> OpenCV
    DETECTOR_BACKEND = "opencv"

    NORMALIZATION = "Facenet"

    # Keep same threshold as old implementation.
    DISTANCE_THRESHOLD = 0.30

    EXPECTED_EMBEDDING_SIZE = 512

    # Phone cameras send large images (several MB, 3000-4000px wide).
    # OpenCV detection + Facenet embedding cost scales with pixel count,
    # so downscaling to this size before detection is the single
    # biggest latency win available without touching model/accuracy.
    MAX_IMAGE_DIMENSION = 800

    # =========================================================
    # COMMON IMAGE DECODER
    # =========================================================

    @staticmethod
    def _decode_image(image_bytes: bytes):

        if not image_bytes:
            raise ValueError("Image file is empty")

        image_array = np.frombuffer(
            image_bytes,
            dtype=np.uint8
        )

        image = cv2.imdecode(
            image_array,
            cv2.IMREAD_COLOR
        )

        if image is None:
            raise ValueError("Invalid image file")

        return FaceService._downscale_if_needed(image)

    @staticmethod
    def _downscale_if_needed(image):

        height, width = image.shape[:2]

        longest_side = max(height, width)

        if longest_side <= FaceService.MAX_IMAGE_DIMENSION:
            return image

        scale = FaceService.MAX_IMAGE_DIMENSION / float(longest_side)

        new_size = (
            max(1, int(width * scale)),
            max(1, int(height * scale))
        )

        return cv2.resize(
            image,
            new_size,
            interpolation=cv2.INTER_AREA
        )

    # =========================================================
    # OPTIONAL MODEL WARM-UP
    #
    # Performance optimization:
    # DeepFace normally loads the model on the first request.
    # Warm-up can move that delay to startup.
    #
    # This does NOT change recognition/security logic.
    # =========================================================

    @classmethod
    def warmup(cls):

        try:

            print(
                "[WARMUP] Loading Facenet512 model..."
            )

            DeepFace.build_model(
                task="facial_recognition",
                model_name=cls.MODEL_NAME
            )

            print(
                "[WARMUP] Facenet512 model loaded successfully"
            )

        except Exception as e:

            # Do not crash the service because warm-up failed.
            # DeepFace can still lazy-load the model on first request.
            print(
                f"[WARMUP] Model warm-up skipped: {str(e)}"
            )

    # =========================================================
    # REGISTERED EMBEDDING VALIDATION
    # =========================================================

    @staticmethod
    def _validate_registered_embedding(
            registered_embedding: list
    ):

        if not isinstance(
            registered_embedding,
            list
        ):

            raise ValueError(
                "Registered face embedding must be a list"
            )

        if not registered_embedding:

            raise ValueError(
                "Registered face embedding is empty"
            )

        if len(registered_embedding) != (
                FaceService.EXPECTED_EMBEDDING_SIZE
        ):

            raise ValueError(
                "Invalid registered embedding size"
            )

        try:

            registered_vector = np.asarray(
                registered_embedding,
                dtype=np.float32
            )

        except Exception as e:

            raise ValueError(
                "Invalid registered face embedding"
            ) from e

        if not np.all(
            np.isfinite(registered_vector)
        ):

            raise ValueError(
                "Registered face embedding contains invalid values"
            )

        norm = np.linalg.norm(
            registered_vector
        )

        if (
            not np.isfinite(norm)
            or norm == 0
        ):

            raise ValueError(
                "Invalid registered face embedding"
            )

        return registered_vector

    # =========================================================
    # EMBEDDING COMPARISON
    # =========================================================

    @staticmethod
    def _compare_embeddings(
            registered_embedding: list,
            current_embedding: list
    ):

        registered_vector = (
            FaceService._validate_registered_embedding(
                registered_embedding
            )
        )

        if not isinstance(
            current_embedding,
            list
        ):

            raise ValueError(
                "Current face embedding must be a list"
            )

        if not current_embedding:

            raise ValueError(
                "Current face embedding is empty"
            )

        if len(current_embedding) != (
                FaceService.EXPECTED_EMBEDDING_SIZE
        ):

            raise ValueError(
                "Invalid current embedding size"
            )

        try:

            current_vector = np.asarray(
                current_embedding,
                dtype=np.float32
            )

        except Exception as e:

            raise ValueError(
                "Invalid current face embedding"
            ) from e

        if not np.all(
            np.isfinite(current_vector)
        ):

            raise ValueError(
                "Current face embedding contains invalid values"
            )

        registered_norm = np.linalg.norm(
            registered_vector
        )

        current_norm = np.linalg.norm(
            current_vector
        )

        if (
            registered_norm == 0
            or current_norm == 0
            or not np.isfinite(registered_norm)
            or not np.isfinite(current_norm)
        ):

            raise ValueError(
                "Invalid face embedding"
            )

        # Cosine similarity
        cosine_similarity = (
            np.dot(
                registered_vector,
                current_vector
            )
            /
            (
                registered_norm
                * current_norm
            )
        )

        # Numerical safety
        cosine_similarity = float(
            np.clip(
                cosine_similarity,
                -1.0,
                1.0
            )
        )

        distance = (
            1.0
            - cosine_similarity
        )

        verified = (
            distance
            <= FaceService.DISTANCE_THRESHOLD
        )

        print(
            f"[FACE MATCH] "
            f"cosine_similarity={cosine_similarity:.6f}, "
            f"distance={distance:.6f}, "
            f"threshold={FaceService.DISTANCE_THRESHOLD:.6f}, "
            f"verified={verified}"
        )

        return {

            "verified":
                bool(verified),

            "distance":
                round(
                    float(distance),
                    4
                ),

            "threshold":
                FaceService.DISTANCE_THRESHOLD,

            "model":
                FaceService.MODEL_NAME
        }

    # =========================================================
    # FACE DETECTION
    #
    # Same DeepFace flow as old code.
    # Only detector backend changed to OpenCV.
    # =========================================================

    @staticmethod
    def detect_faces(
            image_bytes: bytes
    ):

        image = FaceService._decode_image(
            image_bytes
        )

        try:

            faces = DeepFace.extract_faces(

                img_path=image,

                detector_backend=
                    FaceService.DETECTOR_BACKEND,

                enforce_detection=False,

                align=True,

                anti_spoofing=False
            )

            detected_faces = []

            for face in faces:

                facial_area = face.get(
                    "facial_area",
                    {}
                )

                x = int(
                    facial_area.get(
                        "x",
                        0
                    )
                )

                y = int(
                    facial_area.get(
                        "y",
                        0
                    )
                )

                width = int(
                    facial_area.get(
                        "w",
                        0
                    )
                )

                height = int(
                    facial_area.get(
                        "h",
                        0
                    )
                )

                if (
                    width > 0
                    and height > 0
                ):

                    detected_faces.append({

                        "x": x,

                        "y": y,

                        "width": width,

                        "height": height
                    })

            return detected_faces

        except Exception as e:

            raise RuntimeError(
                f"Face detection failed: {str(e)}"
            ) from e

    # =========================================================
    # ANTI-SPOOFING
    #
    # Same old DeepFace anti-spoofing flow.
    # =========================================================

    @staticmethod
    def check_anti_spoofing(
            image_bytes: bytes
    ):

        image = FaceService._decode_image(
            image_bytes
        )

        try:

            faces = DeepFace.extract_faces(

                img_path=image,

                detector_backend=
                    FaceService.DETECTOR_BACKEND,

                enforce_detection=True,

                align=True,

                anti_spoofing=True
            )

            results = []

            for face in faces:

                facial_area = face.get(
                    "facial_area",
                    {}
                )

                is_real = bool(
                    face.get(
                        "is_real",
                        False
                    )
                )

                anti_spoof_score = float(
                    face.get(
                        "antispoof_score",
                        0.0
                    )
                )

                results.append({

                    "isReal":
                        is_real,

                    "antiSpoofScore":
                        round(
                            anti_spoof_score,
                            4
                        ),

                    "face": {

                        "x": int(
                            facial_area.get(
                                "x",
                                0
                            )
                        ),

                        "y": int(
                            facial_area.get(
                                "y",
                                0
                            )
                        ),

                        "width": int(
                            facial_area.get(
                                "w",
                                0
                            )
                        ),

                        "height": int(
                            facial_area.get(
                                "h",
                                0
                            )
                        )
                    }
                })

            # Exactly one face required.
            if len(results) != 1:

                return {

                    "success": True,

                    "faceDetected":
                        len(results) > 0,

                    "faceCount":
                        len(results),

                    "live": False,

                    "faces":
                        results,

                    "message":
                        "Exactly one face is required"
                }

            live = results[0][
                "isReal"
            ]

            return {

                "success": True,

                "faceDetected": True,

                "faceCount": 1,

                "live":
                    live,

                "faces":
                    results,

                "message": (
                    "Live face detected"
                    if live
                    else "Spoof detected"
                )
            }

        except Exception as e:

            raise RuntimeError(
                f"Anti-spoofing failed: {str(e)}"
            ) from e

    # =========================================================
    # GENERATE EMBEDDING
    #
    # Used during registration.
    #
    # Existing DeepFace detection/crop is preserved.
    # =========================================================

    @staticmethod
    def generate_embedding(
            image_bytes: bytes
    ):

        image = FaceService._decode_image(
            image_bytes
        )

        try:

            representations = (
                DeepFace.represent(

                    img_path=image,

                    model_name=
                        FaceService.MODEL_NAME,

                    detector_backend=
                        FaceService.DETECTOR_BACKEND,

                    enforce_detection=True,

                    align=True,

                    normalization=
                        FaceService.NORMALIZATION
                )
            )

            if not representations:

                raise ValueError(
                    "No face detected"
                )

            if len(representations) != 1:

                raise ValueError(
                    "Exactly one face must be visible"
                )

            embedding = (
                representations[0]
                .get("embedding")
            )

            if not embedding:

                raise ValueError(
                    "Unable to generate face embedding"
                )

            if len(embedding) != (
                    FaceService.EXPECTED_EMBEDDING_SIZE
            ):

                raise ValueError(
                    "Invalid generated embedding size"
                )

            return {

                "embedding": [
                    float(value)
                    for value in embedding
                ],

                "embeddingSize":
                    len(embedding),

                "model":
                    FaceService.MODEL_NAME
            }

        except ValueError:

            raise

        except Exception as e:

            raise RuntimeError(
                f"Face embedding failed: {str(e)}"
            ) from e

    # =========================================================
    # VERIFY TWO IMAGES
    #
    # Existing/testing API.
    # =========================================================

    @staticmethod
    def verify_faces(
            image1_bytes: bytes,
            image2_bytes: bytes
    ):

        image1 = FaceService._decode_image(
            image1_bytes
        )

        image2 = FaceService._decode_image(
            image2_bytes
        )

        try:

            result = DeepFace.verify(

                img1_path=image1,

                img2_path=image2,

                model_name=
                    FaceService.MODEL_NAME,

                detector_backend=
                    FaceService.DETECTOR_BACKEND,

                enforce_detection=True,

                align=True,

                normalization=
                    FaceService.NORMALIZATION
            )

            return {

                "verified":
                    bool(
                        result.get(
                            "verified",
                            False
                        )
                    ),

                "distance":
                    round(
                        float(
                            result.get(
                                "distance",
                                1.0
                            )
                        ),
                        4
                    ),

                "threshold":
                    round(
                        float(
                            result.get(
                                "threshold",
                                FaceService.DISTANCE_THRESHOLD
                            )
                        ),
                        4
                    ),

                "model":
                    FaceService.MODEL_NAME
            }

        except Exception as e:

            raise RuntimeError(
                f"Face verification failed: {str(e)}"
            ) from e

    # =========================================================
    # OLD VERIFY EMBEDDING API
    #
    # Kept for backward compatibility.
    # =========================================================

    @staticmethod
    def verify_embedding(
            registered_embedding: list,
            current_image_bytes: bytes
    ):

        FaceService._validate_registered_embedding(
            registered_embedding
        )

        image = FaceService._decode_image(
            current_image_bytes
        )

        try:

            representations = (
                DeepFace.represent(

                    img_path=image,

                    model_name=
                        FaceService.MODEL_NAME,

                    detector_backend=
                        FaceService.DETECTOR_BACKEND,

                    enforce_detection=True,

                    align=True,

                    normalization=
                        FaceService.NORMALIZATION
                )
            )

            if not representations:

                raise ValueError(
                    "No face detected in current image"
                )

            if len(representations) != 1:

                raise ValueError(
                    "Exactly one face must be visible"
                )

            current_embedding = (
                representations[0]
                .get("embedding")
            )

            if not current_embedding:

                raise ValueError(
                    "Unable to generate current face embedding"
                )

            return (
                FaceService._compare_embeddings(

                    registered_embedding,

                    current_embedding
                )
            )

        except ValueError:

            raise

        except Exception as e:

            raise RuntimeError(
                f"Face embedding verification failed: {str(e)}"
            ) from e

    # =========================================================
    # OPTIMIZED ATTENDANCE VERIFICATION
    #
    # SECURITY FLOW:
    #
    # 1. Decode image
    # 2. DeepFace detects face using OpenCV
    # 3. Exactly one real detected face required
    # 4. Anti-spoof verification
    # 5. Reuse DeepFace detected/cropped face
    # 6. Generate Facenet512 embedding
    # 7. Compare ONLY against logged-in user's
    #    registered embedding
    # 8. Return Verified only when every check passes
    #
    # NO MANUAL CROP
    # NO CUSTOM RESIZE
    # NO SECOND FACE DETECTOR
    # =========================================================

    @staticmethod
    def verify_attendance(
            image_bytes: bytes,
            registered_embedding: list
    ):

        # ---------------------------------------------------------
        # Validate registered embedding first.
        #
        # This embedding is supplied by Spring Boot for
        # the authenticated/logged-in student.
        # ---------------------------------------------------------

        FaceService._validate_registered_embedding(
            registered_embedding
        )

        # ---------------------------------------------------------
        # Decode image only once.
        # ---------------------------------------------------------

        image = FaceService._decode_image(
            image_bytes
        )

        try:

            # =====================================================
            # STEP 1
            # FACE DETECTION + ANTI-SPOOF
            #
            # DeepFace performs:
            #
            # OpenCV detection
            #       +
            # face extraction/crop
            #       +
            # alignment
            #       +
            # anti-spoofing
            #
            # No manual/custom crop.
            # =====================================================

            faces = DeepFace.extract_faces(

                img_path=image,

                detector_backend=
                    FaceService.DETECTOR_BACKEND,

                enforce_detection=False,

                align=True,

                anti_spoofing=True
            )

            # =====================================================
            # STEP 2
            # VALIDATE ACTUAL DETECTED FACES
            #
            # enforce_detection=False can return a fallback/dummy
            # result when no face exists.
            #
            # Therefore we validate facial_area ourselves.
            # =====================================================

            valid_faces = []

            for face in faces or []:

                facial_area = face.get(
                    "facial_area",
                    {}
                )

                width = int(
                    facial_area.get(
                        "w",
                        0
                    )
                )

                height = int(
                    facial_area.get(
                        "h",
                        0
                    )
                )

                if (
                    width > 0
                    and height > 0
                ):

                    valid_faces.append(
                        face
                    )

            # =====================================================
            # NO FACE
            # =====================================================

            if len(valid_faces) == 0:

                return {

                    "success": True,

                    "verified": False,

                    "spoof": False,

                    "faceDetected": False,

                    "faceCount": 0,

                    "distance": 1.0,

                    "threshold":
                        FaceService.DISTANCE_THRESHOLD,

                    "antiSpoofScore": 0.0,

                    "model":
                        FaceService.MODEL_NAME,

                    "message":
                        "Unregistered"
                }

            # =====================================================
            # EXACTLY ONE FACE REQUIRED
            #
            # Important security check.
            # Multiple faces are never accepted.
            # =====================================================

            if len(valid_faces) != 1:

                return {

                    "success": True,

                    "verified": False,

                    "spoof": False,

                    "faceDetected": True,

                    "faceCount":
                        len(valid_faces),

                    "distance": 1.0,

                    "threshold":
                        FaceService.DISTANCE_THRESHOLD,

                    "antiSpoofScore": 0.0,

                    "model":
                        FaceService.MODEL_NAME,

                    "message":
                        "Unregistered"
                }

            detected_face = valid_faces[0]

            # =====================================================
            # STEP 3
            # ANTI-SPOOF
            # =====================================================

            is_real = bool(
                detected_face.get(
                    "is_real",
                    False
                )
            )

            anti_spoof_score = float(
                detected_face.get(
                    "antispoof_score",
                    0.0
                )
            )

            # =====================================================
            # FAKE PHOTO / SCREEN / SPOOF
            #
            # Never continue to face matching if spoof detected.
            # =====================================================

            if not is_real:

                return {

                    "success": True,

                    "verified": False,

                    "spoof": True,

                    "faceDetected": True,

                    "faceCount": 1,

                    "distance": 1.0,

                    "threshold":
                        FaceService.DISTANCE_THRESHOLD,

                    "antiSpoofScore":
                        round(
                            anti_spoof_score,
                            4
                        ),

                    "model":
                        FaceService.MODEL_NAME,

                    "message":
                        "Spoof Detected"
                }

            # =====================================================
            # STEP 4
            # GET FACE IMAGE FROM SAME DEEPFACE DETECTION
            #
            # IMPORTANT:
            #
            # We are NOT manually cropping.
            #
            # DeepFace already detected and cropped the face.
            # We simply reuse that result.
            # =====================================================

            face_image = detected_face.get(
                "face"
            )

            if face_image is None:

                raise ValueError(
                    "Unable to extract detected face"
                )

            if not isinstance(
                face_image,
                np.ndarray
            ):

                raise ValueError(
                    "Invalid detected face"
                )

            if face_image.size == 0:

                raise ValueError(
                    "Detected face is empty"
                )

            # =====================================================
            # STEP 5
            # GENERATE EMBEDDING
            #
            # detector_backend="skip"
            #
            # Why?
            #
            # Because DeepFace has ALREADY detected/cropped
            # the face above.
            #
            # Running detector again would waste time and could
            # produce another crop.
            #
            # This does NOT bypass face detection security because
            # the image here is ONLY the face produced by the
            # successful detection above.
            # =====================================================

            representations = (
                DeepFace.represent(

                    img_path=face_image,

                    model_name=
                        FaceService.MODEL_NAME,

                    detector_backend="skip",

                    enforce_detection=False,

                    align=False,

                    normalization=
                        FaceService.NORMALIZATION
                )
            )

            if not representations:

                raise ValueError(
                    "Unable to generate current face embedding"
                )

            if len(representations) != 1:

                raise ValueError(
                    "Invalid face representation"
                )

            current_embedding = (
                representations[0]
                .get("embedding")
            )

            if not current_embedding:

                raise ValueError(
                    "Unable to generate current face embedding"
                )

            if len(current_embedding) != (
                    FaceService.EXPECTED_EMBEDDING_SIZE
            ):

                raise ValueError(
                    "Invalid current embedding size"
                )

            # =====================================================
            # STEP 6
            # COMPARE ONLY WITH LOGGED-IN USER
            #
            # Never search all students here.
            # Only compare against supplied registered embedding.
            # =====================================================

            verification = (
                FaceService._compare_embeddings(

                    registered_embedding,

                    current_embedding
                )
            )

            # =====================================================
            # STEP 7
            # FACE MISMATCH
            # =====================================================

            if not verification["verified"]:

                return {

                    "success": True,

                    "verified": False,

                    "spoof": False,

                    "faceDetected": True,

                    "faceCount": 1,

                    "distance":
                        verification[
                            "distance"
                        ],

                    "threshold":
                        verification[
                            "threshold"
                        ],

                    "antiSpoofScore":
                        round(
                            anti_spoof_score,
                            4
                        ),

                    "model":
                        FaceService.MODEL_NAME,

                    "message":
                        "Unregistered"
                }

            # =====================================================
            # STEP 8
            # FINAL SUCCESS
            #
            # All security checks passed:
            #
            # 1. Face detected
            # 2. Exactly one face
            # 3. Real/live face
            # 4. Valid face embedding
            # 5. Registered embedding valid
            # 6. Face similarity within threshold
            # =====================================================

            return {

                "success": True,

                "verified": True,

                "spoof": False,

                "faceDetected": True,

                "faceCount": 1,

                "distance":
                    verification[
                        "distance"
                    ],

                "threshold":
                    verification[
                        "threshold"
                    ],

                "antiSpoofScore":
                    round(
                        anti_spoof_score,
                        4
                    ),

                "model":
                    FaceService.MODEL_NAME,

                "message":
                    "Verified"
            }

        except ValueError:

            raise

        except Exception as e:

            raise RuntimeError(
                f"Attendance verification failed: {str(e)}"
            ) from e