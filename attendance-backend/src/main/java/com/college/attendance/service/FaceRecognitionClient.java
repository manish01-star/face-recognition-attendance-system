package com.college.attendance.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FaceRecognitionClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${face.service.url}")
    private String faceServiceUrl;


    // =========================================================
    // ANTI SPOOF
    // =========================================================

    public Map<String, Object> checkAntiSpoof(
            MultipartFile file) throws Exception {

        validateFile(file);

        ByteArrayResource resource =
                new ByteArrayResource(file.getBytes()) {

                    @Override
                    public String getFilename() {

                        String filename =
                                file.getOriginalFilename();

                        return filename != null
                                && !filename.isBlank()
                                ? filename
                                : "face.jpg";
                    }
                };


        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "file",
                resource);


        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.MULTIPART_FORM_DATA);

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON));


        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers);


        String url =
                faceServiceUrl
                        + "/api/v1/face/anti-spoof";


        try {

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class);


            if (!response.getStatusCode()
                    .is2xxSuccessful()) {

                throw new RuntimeException(
                        "Face service anti-spoof request failed. HTTP status: "
                                + response.getStatusCode());
            }


            if (response.getBody() == null
                    || response.getBody().isBlank()) {

                throw new RuntimeException(
                        "Empty response from face anti-spoof service");
            }


            return objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );


        } catch (RestClientException e) {

            throw new RuntimeException(
                    "Unable to connect to face recognition service",
                    e
            );
        }
    }


    // =========================================================
    // VERIFY EMBEDDING
    // =========================================================

    /**
     * Verify current image against registered embedding.
     *
     * Python receives:
     *
     * registeredEmbedding = [512 values]
     *
     * current_file = current face image
     */
    public Map<String, Object> verifyEmbedding(

            List<Double> registeredEmbedding,

            MultipartFile file

    ) throws Exception {


        if (registeredEmbedding == null
                || registeredEmbedding.isEmpty()) {

            throw new IllegalArgumentException(
                    "Registered face embedding is required");
        }


        if (registeredEmbedding.size() != 512) {

            throw new IllegalArgumentException(
                    "Invalid registered face embedding. Expected 512 values");
        }


        validateFile(file);


        ByteArrayResource resource =
                new ByteArrayResource(
                        file.getBytes()) {

                    @Override
                    public String getFilename() {

                        String filename =
                                file.getOriginalFilename();

                        return filename != null
                                && !filename.isBlank()
                                ? filename
                                : "face.jpg";
                    }
                };


        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();


        /*
         * Registered face embedding
         */
        body.add(
                "registeredEmbedding",
                objectMapper.writeValueAsString(
                        registeredEmbedding
                )
        );


        /*
         * Current face image
         */
        body.add(
                "current_file",
                resource
        );


        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.MULTIPART_FORM_DATA);

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON));


        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers);


        String url =
                faceServiceUrl
                        + "/api/v1/face/verify-embedding";


        try {

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class);


            if (!response.getStatusCode()
                    .is2xxSuccessful()) {

                throw new RuntimeException(
                        "Face verification service failed. HTTP status: "
                                + response.getStatusCode());
            }


            if (response.getBody() == null
                    || response.getBody().isBlank()) {

                throw new RuntimeException(
                        "Empty response from face verification service");
            }


            return objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );


        } catch (RestClientException e) {

            throw new RuntimeException(
                    "Unable to connect to face recognition service",
                    e
            );
        }
    }


    // =========================================================
    // FILE VALIDATION
    // =========================================================

    private void validateFile(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Face image is required");
        }


        if (file.getContentType() == null
                || !file.getContentType()
                .startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Only image files are allowed");
        }
    }
}