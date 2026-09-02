package com.college.attendance.service;

import com.college.attendance.dto.face.AntiSpoofResponse;
import com.college.attendance.dto.face.FaceRegistrationResponse;
import com.college.attendance.dto.face.FaceServiceResponse;
import com.college.attendance.entity.FaceEmbedding;
import com.college.attendance.entity.User;
import com.college.attendance.repository.FaceEmbeddingRepository;
import com.college.attendance.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FaceRegistrationService {

        private final UserRepository userRepository;

        private final FaceEmbeddingRepository faceEmbeddingRepository;

        private final RestTemplate restTemplate;

        @Value("${face.service.url}")
        private String faceServiceUrl;

        // =========================================================
        // REGISTER FACE
        // =========================================================

        public FaceRegistrationResponse register(
                        Long userId,
                        MultipartFile file) throws Exception {

                // =====================================================
                // 1. Validate file
                // =====================================================

                if (file == null || file.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Face image is required");
                }

                if (file.getContentType() == null
                                || !file.getContentType()
                                                .startsWith("image/")) {

                        throw new IllegalArgumentException(
                                        "Only image files are allowed");
                }

                // =====================================================
                // 2. Find User
                // =====================================================

                User user = userRepository
                                .findById(userId)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found"));

                // =====================================================
                // 3. Check active user
                // =====================================================

                if (user.getStatus() == null
                                || !"ACTIVE".equals(
                                                user.getStatus().name())) {

                        throw new RuntimeException(
                                        "User is inactive");
                }

                byte[] imageBytes = file.getBytes();

                // =====================================================
                // 4. Anti Spoofing
                // =====================================================

                AntiSpoofResponse antiSpoofResponse = checkAntiSpoofing(
                                imageBytes,
                                file.getOriginalFilename());

                if (!antiSpoofResponse.isSuccess()) {

                        throw new RuntimeException(
                                        "Anti-spoofing failed: "
                                                        + antiSpoofResponse.getMessage());
                }

                if (!antiSpoofResponse.isFaceDetected()) {

                        throw new IllegalArgumentException(
                                        "No face detected in image");
                }

                if (antiSpoofResponse.getFaceCount() != 1) {

                        throw new IllegalArgumentException(
                                        "Exactly one face is required");
                }

                if (!antiSpoofResponse.isLive()) {

                        throw new IllegalArgumentException(
                                        "Spoof detected. Please use a live face.");
                }

                // =====================================================
                // 5. Generate Face Embedding
                // =====================================================

                FaceServiceResponse faceResponse = generateEmbedding(
                                imageBytes,
                                file.getOriginalFilename());

                if (faceResponse == null
                                || !faceResponse.isSuccess()) {

                        throw new RuntimeException(
                                        "Face embedding generation failed");
                }

                if (!faceResponse.isFaceDetected()) {

                        throw new IllegalArgumentException(
                                        "No face detected in image");
                }

                if (faceResponse.getFaceCount() != 1) {

                        throw new IllegalArgumentException(
                                        "Exactly one face is required");
                }

                if (faceResponse.getEmbedding() == null
                                || faceResponse.getEmbedding().isEmpty()) {

                        throw new RuntimeException(
                                        "Face embedding is empty");
                }

                // =====================================================
                // 6. Convert embedding to JSON
                // =====================================================

                String embeddingJson = faceResponse.getEmbedding()
                                .stream()
                                .map(String::valueOf)
                                .collect(
                                                Collectors.joining(
                                                                ",",
                                                                "[",
                                                                "]"));

                // =====================================================
                // 7. Find existing FaceEmbedding
                // =====================================================

                FaceEmbedding faceEmbedding = faceEmbeddingRepository
                                .findByUserId(userId)
                                .orElse(null);

                // =====================================================
                // 8. Create new / Update existing
                // =====================================================

                if (faceEmbedding == null) {

                        faceEmbedding = FaceEmbedding.builder()
                                        .user(user)
                                        .embedding(embeddingJson)
                                        .embeddingSize(
                                                        faceResponse.getEmbeddingSize())
                                        .modelName(
                                                        faceResponse.getModel())
                                        .detectorBackend(
                                                        "retinaface")
                                        .status("ACTIVE")
                                        .build();

                } else {

                        faceEmbedding.setEmbedding(
                                        embeddingJson);

                        faceEmbedding.setEmbeddingSize(
                                        faceResponse.getEmbeddingSize());

                        faceEmbedding.setModelName(
                                        faceResponse.getModel());

                        faceEmbedding.setDetectorBackend(
                                        "retinaface");

                        faceEmbedding.setStatus(
                                        "ACTIVE");
                }

                // =====================================================
                // 9. Save
                // =====================================================

                faceEmbeddingRepository.save(
                                faceEmbedding);

                // =====================================================
                // 10. Response
                // =====================================================

                return FaceRegistrationResponse.builder()
                                .userId(user.getId())
                                .name(user.getUsername())
                                .role(user.getRole().name())
                                .modelName(
                                                faceResponse.getModel())
                                .status("REGISTERED")
                                .message(
                                                "Face registered successfully")
                                .build();
        }

        // =========================================================
        // ANTI SPOOFING
        // =========================================================

        private AntiSpoofResponse checkAntiSpoofing(
                        byte[] imageBytes,
                        String fileName) {

                return sendImageToPython(
                                "/api/v1/face/anti-spoof",
                                imageBytes,
                                fileName,
                                AntiSpoofResponse.class);
        }

        // =========================================================
        // FACE EMBEDDING
        // =========================================================

        private FaceServiceResponse generateEmbedding(
                        byte[] imageBytes,
                        String fileName) {

                return sendImageToPython(
                                "/api/v1/face/embedding",
                                imageBytes,
                                fileName,
                                FaceServiceResponse.class);
        }

        // =========================================================
        // COMMON PYTHON REQUEST
        // =========================================================

        private <T> T sendImageToPython(
                        String endpoint,
                        byte[] imageBytes,
                        String fileName,
                        Class<T> responseType) {

                ByteArrayResource resource = new ByteArrayResource(imageBytes) {

                        @Override
                        public String getFilename() {

                                if (fileName == null
                                                || fileName.isBlank()) {
                                        return "face.jpg";
                                }

                                return fileName;
                        }
                };

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

                body.add(
                                "file",
                                resource);

                HttpHeaders headers = new HttpHeaders();

                headers.setContentType(
                                MediaType.MULTIPART_FORM_DATA);

                headers.setAccept(
                                java.util.List.of(
                                                MediaType.APPLICATION_JSON));

                HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(
                                body,
                                headers);

                String url = faceServiceUrl + endpoint;

                ResponseEntity<T> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                request,
                                responseType);

                if (!response.getStatusCode()
                                .is2xxSuccessful()
                                || response.getBody() == null) {

                        throw new RuntimeException(
                                        "Face service returned invalid response");
                }

                return response.getBody();
        }
}