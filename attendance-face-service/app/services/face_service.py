import cv2
import numpy as np

from deepface import DeepFace


class FaceService:

    @staticmethod
    def detect_faces(image_bytes: bytes):

        image_array = np.frombuffer(image_bytes, np.uint8)

        image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

        if image is None:
            raise ValueError("Invalid image file")

        try:

            faces = DeepFace.extract_faces(
                img_path=image,
                detector_backend="retinaface",
                enforce_detection=False,
                align=True
            )

            detected_faces = []

            for face in faces:

                facial_area = face.get("facial_area", {})

                x = int(facial_area.get("x", 0))
                y = int(facial_area.get("y", 0))
                width = int(facial_area.get("w", 0))
                height = int(facial_area.get("h", 0))

                if width > 0 and height > 0:

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
            )

    @staticmethod
    def check_anti_spoofing(image_bytes: bytes):

        image_array = np.frombuffer(image_bytes, np.uint8)

        image = cv2.imdecode(
            image_array,
            cv2.IMREAD_COLOR
        )

        if image is None:
            raise ValueError("Invalid image file")

        try:

            faces = DeepFace.extract_faces(
                img_path=image,
                detector_backend="retinaface",
                enforce_detection=True,
                align=True,
                anti_spoofing=True
            )

            results = []

            for face in faces:

                is_real = face.get(
                    "is_real",
                    False
                )

                antispoof_score = face.get(
                    "antispoof_score",
                    0.0
                )

                facial_area = face.get(
                    "facial_area",
                    {}
                )

                results.append({
                    "isReal": bool(is_real),
                    "antiSpoofScore": round(
                        float(antispoof_score),
                        2
                    ),
                    "face": {
                        "x": int(
                            facial_area.get("x", 0)
                        ),
                        "y": int(
                            facial_area.get("y", 0)
                        ),
                        "width": int(
                            facial_area.get("w", 0)
                        ),
                        "height": int(
                            facial_area.get("h", 0)
                        )
                    }
                })

            real_faces = [
                face
                for face in results
                if face["isReal"]
            ]

            return {
                "success": True,
                "faceDetected": len(results) > 0,
                "faceCount": len(results),
                "live": len(real_faces) > 0,
                "faces": results,
                "message": (
                    "Live face detected"
                    if len(real_faces) > 0
                    else "Spoof detected"
                )
            }

        except Exception as e:

            raise RuntimeError(
                f"Anti-spoofing failed: {str(e)}"
            )

    @staticmethod
    def generate_embedding(image_bytes: bytes):

        image_array = np.frombuffer(
            image_bytes,
            np.uint8
        )

        image = cv2.imdecode(
            image_array,
            cv2.IMREAD_COLOR
        )

        if image is None:
            raise ValueError(
                "Invalid image file"
            )

        try:

            representations = DeepFace.represent(
                img_path=image,
                model_name="Facenet512",
                detector_backend="retinaface",
                enforce_detection=True,
                align=True,
                normalization="Facenet"
            )

            if not representations:

                raise ValueError(
                    "No face detected"
                )

            # We expect one face during registration
            if len(representations) > 1:

                raise ValueError(
                    "Multiple faces detected. "
                    "Please upload an image containing "
                    "only one face."
                )

            embedding = representations[0].get(
                "embedding"
            )

            if not embedding:

                raise ValueError(
                    "Unable to generate face embedding"
                )

            return {
                "embedding": [
                    float(value)
                    for value in embedding
                ],
                "embeddingSize": len(embedding),
                "model": "Facenet512"
            }

        except ValueError:
            raise

        except Exception as e:

            raise RuntimeError(
                f"Face embedding failed: {str(e)}"
            )


    @staticmethod
    def verify_faces(
        image1_bytes: bytes,
        image2_bytes: bytes
    ):

        image1_array = np.frombuffer(
            image1_bytes,
            np.uint8
        )

        image2_array = np.frombuffer(
            image2_bytes,
            np.uint8
        )

        image1 = cv2.imdecode(
            image1_array,
            cv2.IMREAD_COLOR
        )

        image2 = cv2.imdecode(
            image2_array,
            cv2.IMREAD_COLOR
        )

        if image1 is None:
            raise ValueError("Invalid first image")

        if image2 is None:
            raise ValueError("Invalid second image")

        try:

            result = DeepFace.verify(
                img1_path=image1,
                img2_path=image2,
                model_name="Facenet512",
                detector_backend="retinaface",
                enforce_detection=True,
                align=True
            )

            distance = float(
                result.get("distance", 0.0)
            )

            threshold = float(
                result.get("threshold", 0.30)
            )

            verified = bool(
                result.get("verified", False)
            )

            return {
                "verified": verified,
                "distance": round(distance, 4),
                "threshold": round(threshold, 4),
                "model": "Facenet512"
            }

        except Exception as e:

            raise RuntimeError(
                f"Face verification failed: {str(e)}"
            )

    @staticmethod
    def verify_embedding(
        registered_embedding: list,
        current_image_bytes: bytes
    ):

        image_array = np.frombuffer(
            current_image_bytes,
            np.uint8
        )

        image = cv2.imdecode(
            image_array,
            cv2.IMREAD_COLOR
        )

        if image is None:
            raise ValueError(
                "Invalid current image"
            )

        if not registered_embedding:

            raise ValueError(
                "Registered face embedding is empty"
            )

        try:

            # Generate embedding from current camera image
            representations = DeepFace.represent(
                img_path=image,
                model_name="Facenet512",
                detector_backend="retinaface",
                enforce_detection=True,
                align=True,
                normalization="Facenet"
            )

            if not representations:

                raise ValueError(
                    "No face detected in current image"
                )

            # Attendance should contain only one face
            if len(representations) > 1:

                raise ValueError(
                    "Multiple faces detected. "
                    "Please ensure only one face is visible."
                )

            current_embedding = representations[0].get(
                "embedding"
            )

            if not current_embedding:

                raise ValueError(
                    "Unable to generate current face embedding"
                )

            if len(current_embedding) != len(registered_embedding):

                raise ValueError(
                    "Embedding size mismatch"
                )

            # Calculate cosine distance
            registered_vector = np.array(
                registered_embedding,
                dtype=np.float32
            )

            current_vector = np.array(
                current_embedding,
                dtype=np.float32
            )

            registered_norm = np.linalg.norm(
                registered_vector
            )

            current_norm = np.linalg.norm(
                current_vector
            )

            if registered_norm == 0 or current_norm == 0:

                raise ValueError(
                    "Invalid face embedding"
                )

            cosine_similarity = (
                np.dot(
                    registered_vector,
                    current_vector
                )
                /
                (
                    registered_norm
                    *
                    current_norm
                )
            )

            distance = 1.0 - cosine_similarity

            # Facenet512 cosine threshold
            threshold = 0.30

            verified = distance <= threshold

            return {

                "verified": bool(verified),

                "distance": round(
                    float(distance),
                    4
                ),

                "threshold": threshold,

                "model": "Facenet512"

            }

        except ValueError:

            raise

        except Exception as e:

            raise RuntimeError(
                f"Face embedding verification failed: {str(e)}"
            )            