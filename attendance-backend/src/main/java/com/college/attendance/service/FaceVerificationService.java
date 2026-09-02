package com.college.attendance.service;

import com.college.attendance.dto.face.FaceVerificationResult;
import com.college.attendance.entity.FaceEmbedding;
import com.college.attendance.repository.FaceEmbeddingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceVerificationService {

    private final FaceEmbeddingRepository faceEmbeddingRepository;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${face.service.url}")
    private String faceServiceUrl;


    /**
     * Verify current face against registered face embedding.
     */
    public FaceVerificationResult verify(
            Long userId,
            MultipartFile currentFile) throws Exception {

        log.info("=================================================");
        log.info("FACE VERIFICATION STARTED");
        log.info("User ID       : {}", userId);
        log.info("File Name     : {}", currentFile != null
                ? currentFile.getOriginalFilename()
                : "NULL");
        log.info("File Size     : {}", currentFile != null
                ? currentFile.getSize()
                : 0);
        log.info("=================================================");


        // =====================================================
        // 1. FIND REGISTERED FACE
        // =====================================================

        FaceEmbedding faceEmbedding =
                faceEmbeddingRepository
                        .findByUserIdAndStatus(
                                userId,
                                "ACTIVE")
                        .orElseThrow(() -> {

                            log.warn(
                                    "NO ACTIVE FACE EMBEDDING FOUND FOR USER ID: {}",
                                    userId);

                            return new RuntimeException(
                                    "Face is not registered for this user");
                        });

        log.info(
                "Registered face embedding found for userId: {}",
                userId);


        // =====================================================
        // 2. CONVERT DB JSON -> LIST<Double>
        // =====================================================

        List<Double> registeredEmbedding =
                objectMapper.readValue(
                        faceEmbedding.getEmbedding(),
                        new TypeReference<List<Double>>() {
                        });


        log.info(
                "Registered embedding size: {}",
                registeredEmbedding.size());


        // =====================================================
        // 3. VALIDATE EMBEDDING
        // =====================================================

        if (registeredEmbedding.size() != 512) {

            log.error(
                    "INVALID REGISTERED EMBEDDING SIZE. Expected: 512, Actual: {}",
                    registeredEmbedding.size());

            throw new RuntimeException(
                    "Invalid registered face embedding");
        }


        // =====================================================
        // 4. PREPARE CURRENT IMAGE
        // =====================================================

        if (currentFile == null || currentFile.isEmpty()) {

            log.error("Current face image is empty");

            throw new RuntimeException(
                    "Current face image is required");
        }


        ByteArrayResource resource =
                new ByteArrayResource(
                        currentFile.getBytes()) {

                    @Override
                    public String getFilename() {

                        String filename =
                                currentFile.getOriginalFilename();

                        return filename != null
                                ? filename
                                : "face.jpg";
                    }
                };


        // =====================================================
        // 5. CREATE MULTIPART BODY
        // =====================================================

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();


        // Current image
        body.add(
                "current_file",
                resource);


        // Registered embedding
        String embeddingJson =
                objectMapper.writeValueAsString(
                        registeredEmbedding);

        body.add(
                "registeredEmbedding",
                embeddingJson);


        log.info(
                "Multipart body prepared successfully");

        log.info(
                "current_file added: true");

        log.info(
                "registeredEmbedding added: true");

        log.info(
                "Embedding JSON length: {}",
                embeddingJson.length());


        // =====================================================
        // 6. REQUEST HEADERS
        // =====================================================

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.MULTIPART_FORM_DATA);

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON));


        // =====================================================
        // 7. CREATE REQUEST
        // =====================================================
        //
        // IMPORTANT:
        // Body is completely prepared BEFORE creating HttpEntity.
        // =====================================================

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers);


        // =====================================================
        // 8. PYTHON API URL
        // =====================================================

        String url =
                faceServiceUrl
                        + "/api/v1/face/verify-embedding";


        log.info("Calling Python Face API");
        log.info("Python API URL: {}", url);


        // =====================================================
        // 9. CALL PYTHON API
        // =====================================================

        ResponseEntity<Map> response;

        try {

            response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            Map.class);

        } catch (Exception e) {

            log.error(
                    "ERROR WHILE CALLING PYTHON FACE API",
                    e);

            throw new RuntimeException(
                    "Face verification service unavailable",
                    e);
        }


        // =====================================================
        // 10. PYTHON HTTP RESPONSE
        // =====================================================

        log.info("=================================================");
        log.info("PYTHON FACE API RESPONSE");
        log.info("HTTP STATUS : {}", response.getStatusCode());
        log.info("HTTP BODY   : {}", response.getBody());
        log.info("=================================================");


        Map responseBody =
                response.getBody();


        if (responseBody == null) {

            log.error(
                    "Python face service returned EMPTY response");

            throw new RuntimeException(
                    "Empty response from face service");
        }


        // =====================================================
        // 11. LOG EACH RESPONSE FIELD
        // =====================================================

        Object verifiedObject =
                responseBody.get("verified");

        Object distanceObject =
                responseBody.get("distance");

        Object thresholdObject =
                responseBody.get("threshold");

        Object modelObject =
                responseBody.get("model");

        Object messageObject =
                responseBody.get("message");


        log.info("--------------- PYTHON RESPONSE FIELDS ---------------");

        log.info(
                "verified  : {}",
                verifiedObject);

        log.info(
                "distance  : {}",
                distanceObject);

        log.info(
                "threshold : {}",
                thresholdObject);

        log.info(
                "model     : {}",
                modelObject);

        log.info(
                "message   : {}",
                messageObject);

        log.info("--------------------------------------------------------");


        // =====================================================
        // 12. VALIDATE VERIFIED
        // =====================================================

        boolean verified =
                Boolean.TRUE.equals(
                        verifiedObject);


        // =====================================================
        // 13. VALIDATE DISTANCE
        // =====================================================

        if (!(distanceObject instanceof Number)) {

            log.error(
                    "Invalid/missing distance from Python. Value: {}",
                    distanceObject);

            throw new RuntimeException(
                    "Invalid face verification response: distance missing");
        }


        double distance =
                ((Number) distanceObject)
                        .doubleValue();


        // =====================================================
        // 14. VALIDATE THRESHOLD
        // =====================================================

        if (!(thresholdObject instanceof Number)) {

            log.error(
                    "Invalid/missing threshold from Python. Value: {}",
                    thresholdObject);

            throw new RuntimeException(
                    "Invalid face verification response: threshold missing");
        }


        double threshold =
                ((Number) thresholdObject)
                        .doubleValue();


        // =====================================================
        // 15. MODEL
        // =====================================================

        String model =
                modelObject != null
                        ? String.valueOf(modelObject)
                        : null;


        // =====================================================
        // 16. MESSAGE
        // =====================================================

        String message;

        if (messageObject != null) {

            message =
                    String.valueOf(messageObject);

        } else {

            message =
                    verified
                            ? "Same person"
                            : "Different person";
        }


        // =====================================================
        // 17. FINAL RESULT LOG
        // =====================================================

        log.info("=================================================");
        log.info("FACE VERIFICATION RESULT");
        log.info("User ID   : {}", userId);
        log.info("Verified  : {}", verified);
        log.info("Distance  : {}", distance);
        log.info("Threshold : {}", threshold);
        log.info("Model     : {}", model);
        log.info("Message   : {}", message);
        log.info("=================================================");


        // =====================================================
        // 18. RETURN RESULT
        // =====================================================

        return FaceVerificationResult.builder()

                .userId(userId)

                .verified(verified)

                .distance(distance)

                .threshold(threshold)

                .model(model)

                .message(message)

                .build();
    }
}
