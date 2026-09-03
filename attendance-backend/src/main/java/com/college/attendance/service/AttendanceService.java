package com.college.attendance.service;

import com.college.attendance.dto.attendance.AttendanceMarkResponse;
import com.college.attendance.dto.attendance.AttendanceRequest;
import com.college.attendance.dto.attendance.AttendanceResponse;
import com.college.attendance.dto.attendance.AttendanceUserResponse;
import com.college.attendance.dto.face.FaceVerificationResult;
import com.college.attendance.entity.Attendance;
import com.college.attendance.entity.FaceEmbedding;
import com.college.attendance.entity.Student;
import com.college.attendance.entity.Teacher;
import com.college.attendance.entity.User;
import com.college.attendance.entity.enums.AttendanceSource;
import com.college.attendance.entity.enums.AttendanceStatus;
import com.college.attendance.exception.AttendanceException;
import com.college.attendance.exception.FaceVerificationException;
import com.college.attendance.exception.ResourceNotFoundException;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.FaceEmbeddingRepository;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.TeacherRepository;
import com.college.attendance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    private final UserRepository userRepository;

    private final StudentRepository studentRepository;

    private final TeacherRepository teacherRepository;

    private final FaceEmbeddingRepository faceEmbeddingRepository;

    private final FaceRecognitionClient faceRecognitionClient;

    private final ObjectMapper objectMapper;


    // ============================================================
    // ATTENDANCE LOCATION CONFIGURATION
    // ============================================================

    @Value("${attendance.location.latitude}")
    private double collegeLatitude;

    @Value("${attendance.location.longitude}")
    private double collegeLongitude;

    @Value("${attendance.location.radius-meters:100}")
    private double attendanceRadiusMeters;


    // ============================================================
    // ADMIN - CREATE ATTENDANCE
    // ============================================================

    @Transactional
    public AttendanceResponse createAttendance(
            AttendanceRequest request) {

        if (request == null) {
            throw new AttendanceException(
                    "Attendance request is required");
        }

        if (request.getUserId() == null) {
            throw new AttendanceException(
                    "User ID is required");
        }

        if (request.getAttendanceDate() == null) {
            throw new AttendanceException(
                    "Attendance date is required");
        }

        User user = userRepository.findById(
                request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));

        if (attendanceRepository
                .existsByUserIdAndAttendanceDate(
                        request.getUserId(),
                        request.getAttendanceDate())) {

            throw new AttendanceException(
                    "Attendance already exists for this user and date");
        }

        Attendance attendance = new Attendance();

        attendance.setUser(user);

        attendance.setAttendanceDate(
                request.getAttendanceDate());

        attendance.setCheckInTime(
                request.getCheckInTime());

        attendance.setCheckOutTime(
                request.getCheckOutTime());

        attendance.setCheckInLatitude(
                request.getCheckInLatitude());

        attendance.setCheckInLongitude(
                request.getCheckInLongitude());

        attendance.setCheckOutLatitude(
                request.getCheckOutLatitude());

        attendance.setCheckOutLongitude(
                request.getCheckOutLongitude());

        attendance.setStatus(
                request.getStatus());

        attendance.setConfidence(
                request.getConfidence());

        attendance.setSource(
                request.getSource() != null
                        ? request.getSource()
                        : AttendanceSource.MOBILE);

        attendance = attendanceRepository.save(attendance);

        return toResponse(attendance);
    }


    // ============================================================
    // ADMIN - GET ATTENDANCE BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {

        if (id == null) {
            throw new AttendanceException(
                    "Attendance ID is required");
        }

        Attendance attendance =
                attendanceRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Attendance not found"));

        return toResponse(attendance);
    }


    // ============================================================
    // ADMIN - GET STUDENT ATTENDANCE
    // ============================================================

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendance(
            Long userId) {

        if (userId == null) {
            throw new AttendanceException(
                    "User ID is required");
        }

        userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));

        return attendanceRepository
                .findByUserIdOrderByAttendanceDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // ADMIN - GET USER ATTENDANCE
    // ============================================================

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getUserAttendance(
            Long userId) {

        if (userId == null) {
            throw new AttendanceException(
                    "User ID is required");
        }

        userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));

        return attendanceRepository
                .findByUserIdOrderByAttendanceDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // ADMIN - GET ATTENDANCE BY DATE
    // ============================================================

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(
            LocalDate date) {

        if (date == null) {
            throw new AttendanceException(
                    "Attendance date is required");
        }

        return attendanceRepository
                .findByAttendanceDateOrderByCheckInTimeAsc(date)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // ADMIN - GET USERS FOR ATTENDANCE
    // ============================================================

    @Transactional(readOnly = true)
    public List<AttendanceUserResponse> getAttendanceUsers(

            String type,

            String search,

            Long courseId,

            Long semesterId,

            Long sectionId,

            Long departmentId) {

        if (search != null) {

            search = search.trim();

            if (search.isEmpty()) {
                search = null;
            }
        }

        if (type != null) {

            type = type.trim()
                    .toUpperCase();

            if (type.isEmpty()) {
                type = null;
            }
        }

        List<AttendanceUserResponse> result =
                new ArrayList<>();


        // ========================================================
        // STUDENTS
        // ========================================================

        if (type == null
                || type.equals("ALL")
                || type.equals("STUDENT")) {

            List<Student> students =
                    studentRepository.searchAttendanceStudents(
                            search,
                            courseId,
                            semesterId,
                            sectionId,
                            departmentId);

            for (Student student : students) {

                if (student == null
                        || student.getUser() == null) {

                    continue;
                }

                result.add(
                        AttendanceUserResponse.builder()

                                .userId(
                                        student.getUser().getId())

                                .name(
                                        student.getName())

                                .type(
                                        "STUDENT")

                                .rollNumber(
                                        student.getRollNumber())

                                .courseName(
                                        student.getCourse() != null
                                                ? student.getCourse().getName()
                                                : null)

                                .courseCode(
                                        student.getCourse() != null
                                                ? student.getCourse().getCode()
                                                : null)

                                .semester(
                                        student.getSemester() != null
                                                ? student.getSemester()
                                                        .getSemesterNumber()
                                                : null)

                                .sectionName(
                                        student.getSection() != null
                                                ? student.getSection().getName()
                                                : null)

                                .departmentName(
                                        student.getCourse() != null
                                                && student.getCourse()
                                                        .getDepartment() != null
                                                ? student.getCourse()
                                                        .getDepartment()
                                                        .getName()
                                                : null)

                                .employeeCode(null)

                                .build());
            }
        }


        // ========================================================
        // STAFF / TEACHERS
        // ========================================================

        if (type == null
                || type.equals("ALL")
                || type.equals("STAFF")) {

            List<Teacher> teachers =
                    teacherRepository.searchAttendanceTeachers(
                            search,
                            departmentId);

            for (Teacher teacher : teachers) {

                if (teacher == null
                        || teacher.getUser() == null) {

                    continue;
                }

                result.add(
                        AttendanceUserResponse.builder()

                                .userId(
                                        teacher.getUser().getId())

                                .name(
                                        teacher.getName())

                                .type(
                                        "STAFF")

                                .employeeCode(
                                        teacher.getEmployeeCode())

                                .departmentName(
                                        teacher.getDepartment() != null
                                                ? teacher.getDepartment()
                                                        .getName()
                                                : null)

                                .build());
            }
        }


        // ========================================================
        // SORT BY NAME
        // ========================================================

        result.sort(
                Comparator.comparing(
                        AttendanceUserResponse::getName,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER)));

        return result;
    }


    // ============================================================
    // MOBILE - CHECK IN
    // ============================================================

    @Transactional
    public AttendanceMarkResponse checkIn(

            MultipartFile file,

            Double latitude,

            Double longitude

    ) throws Exception {

        // Validate GPS + college geofence
        validateLocation(
                latitude,
                longitude);

        // JWT user
        User user = getLoggedInUser();

        // Face verification
        FaceVerificationResult faceResult =
                verifyFace(
                        user.getId(),
                        file);

        // Today
        LocalDate today =
                LocalDate.now();

        // Existing attendance
        Attendance attendance =
                attendanceRepository
                        .findByUserIdAndAttendanceDate(
                                user.getId(),
                                today)
                        .orElse(null);

        // Already checked in
        if (attendance != null
                && attendance.getCheckInTime() != null) {

            return buildAlreadyMarkedResponse(
                    attendance,
                    "CHECK_IN",
                    "You have already checked in today");
        }

        // Create attendance
        if (attendance == null) {

            attendance = new Attendance();

            attendance.setUser(user);

            attendance.setAttendanceDate(today);
        }

        attendance.setCheckInTime(
                LocalTime.now());

        attendance.setCheckInLatitude(
                latitude);

        attendance.setCheckInLongitude(
                longitude);

        attendance.setStatus(
                AttendanceStatus.PRESENT);

        attendance.setConfidence(
                calculateConfidence(faceResult));

        attendance.setSource(
                AttendanceSource.MOBILE);

        attendance = attendanceRepository.save(
                attendance);

        return buildAttendanceResponse(
                attendance,
                "CHECK_IN",
                "Attendance marked successfully",
                faceResult,
                latitude,
                longitude);
    }


    // ============================================================
    // MOBILE - CHECK OUT
    // ============================================================

    @Transactional
    public AttendanceMarkResponse checkOut(

            MultipartFile file,

            Double latitude,

            Double longitude

    ) throws Exception {

        // Validate GPS + college geofence
        validateLocation(
                latitude,
                longitude);

        // JWT user
        User user = getLoggedInUser();

        // Face verification
        FaceVerificationResult faceResult =
                verifyFace(
                        user.getId(),
                        file);

        LocalDate today =
                LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByUserIdAndAttendanceDate(
                                user.getId(),
                                today)
                        .orElseThrow(() ->
                                new AttendanceException(
                                        "Please check-in first"));

        if (attendance.getCheckInTime() == null) {

            throw new AttendanceException(
                    "Please check-in first");
        }

        if (attendance.getCheckOutTime() != null) {

            return buildAlreadyMarkedResponse(
                    attendance,
                    "CHECK_OUT",
                    "You have already checked out today");
        }

        attendance.setCheckOutTime(
                LocalTime.now());

        attendance.setCheckOutLatitude(
                latitude);

        attendance.setCheckOutLongitude(
                longitude);

        attendance.setSource(
                AttendanceSource.MOBILE);

        attendance.setConfidence(
                calculateConfidence(faceResult));

        attendance = attendanceRepository.save(
                attendance);

        return buildAttendanceResponse(
                attendance,
                "CHECK_OUT",
                "Check-out successful",
                faceResult,
                latitude,
                longitude);
    }


    // ============================================================
    // MACHINE - CHECK IN
    // ============================================================

    @Transactional
    public AttendanceMarkResponse machineCheckIn(

            Long userId,

            MultipartFile file

    ) throws Exception {

        if (userId == null) {

            throw new AttendanceException(
                    "User ID is required");
        }

        validateFaceFile(file);

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        validateActiveUser(user);

        FaceVerificationResult faceResult =
                verifyFace(
                        userId,
                        file);

        LocalDate today =
                LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByUserIdAndAttendanceDate(
                                userId,
                                today)
                        .orElse(null);

        if (attendance != null
                && attendance.getCheckInTime() != null) {

            return buildAlreadyMarkedResponse(
                    attendance,
                    "CHECK_IN",
                    "Attendance already marked today");
        }

        if (attendance == null) {

            attendance = new Attendance();

            attendance.setUser(user);

            attendance.setAttendanceDate(today);
        }

        attendance.setCheckInTime(
                LocalTime.now());

        // Machine has no GPS
        attendance.setCheckInLatitude(null);
        attendance.setCheckInLongitude(null);

        attendance.setStatus(
                AttendanceStatus.PRESENT);

        attendance.setConfidence(
                calculateConfidence(faceResult));

        attendance.setSource(
                AttendanceSource.MACHINE);

        attendance = attendanceRepository.save(
                attendance);

        return buildAttendanceResponse(
                attendance,
                "CHECK_IN",
                "Machine attendance marked successfully",
                faceResult,
                null,
                null);
    }


    // ============================================================
    // MACHINE - CHECK OUT
    // ============================================================

    @Transactional
    public AttendanceMarkResponse machineCheckOut(

            Long userId,

            MultipartFile file

    ) throws Exception {

        if (userId == null) {

            throw new AttendanceException(
                    "User ID is required");
        }

        validateFaceFile(file);

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        validateActiveUser(user);

        FaceVerificationResult faceResult =
                verifyFace(
                        userId,
                        file);

        LocalDate today =
                LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByUserIdAndAttendanceDate(
                                userId,
                                today)
                        .orElseThrow(() ->
                                new AttendanceException(
                                        "Please check-in first"));

        if (attendance.getCheckInTime() == null) {

            throw new AttendanceException(
                    "Please check-in first");
        }

        if (attendance.getCheckOutTime() != null) {

            return buildAlreadyMarkedResponse(
                    attendance,
                    "CHECK_OUT",
                    "Attendance already checked out today");
        }

        attendance.setCheckOutTime(
                LocalTime.now());

        // Machine has no GPS
        attendance.setCheckOutLatitude(null);
        attendance.setCheckOutLongitude(null);

        attendance.setSource(
                AttendanceSource.MACHINE);

        attendance.setConfidence(
                calculateConfidence(faceResult));

        attendance = attendanceRepository.save(
                attendance);

        return buildAttendanceResponse(
                attendance,
                "CHECK_OUT",
                "Machine check-out successful",
                faceResult,
                null,
                null);
    }


    // ============================================================
    // FACE VERIFICATION
    // ============================================================

    private FaceVerificationResult verifyFace(

            Long userId,

            MultipartFile file

    ) throws Exception {

        validateFaceFile(file);

        if (userId == null) {

            throw new FaceVerificationException(
                    "User ID is required for face verification");
        }


        // ========================================================
        // 1. FIND REGISTERED FACE
        // ========================================================

        FaceEmbedding faceEmbedding =
                faceEmbeddingRepository
                        .findByUserIdAndStatus(
                                userId,
                                "ACTIVE")
                        .orElseThrow(() ->
                                new FaceVerificationException(
                                        "Face is not registered for this user"));


        // ========================================================
        // 2. READ REGISTERED EMBEDDING
        // ========================================================

        String embeddingJson =
                faceEmbedding.getEmbedding();

        if (embeddingJson == null
                || embeddingJson.isBlank()) {

            throw new FaceVerificationException(
                    "Registered face data is empty");
        }

        List<Double> registeredEmbedding;

        try {

            registeredEmbedding =
                    objectMapper.readValue(
                            embeddingJson,
                            new TypeReference<List<Double>>() {
                            });

        } catch (Exception ex) {

            log.error(
                    "Unable to parse registered face embedding for userId {}",
                    userId,
                    ex);

            throw new FaceVerificationException(
                    "Registered face data is invalid");
        }

        if (registeredEmbedding == null
                || registeredEmbedding.size() != 512) {

            throw new FaceVerificationException(
                    "Invalid registered face embedding");
        }


        // ========================================================
        // 3. ANTI SPOOF
        // ========================================================

        Map<String, Object> antiSpoofResponse;

        try {

            antiSpoofResponse =
                    faceRecognitionClient.checkAntiSpoof(
                            file);

        } catch (Exception ex) {

            log.error(
                    "Anti-spoof service failed for userId {}",
                    userId,
                    ex);

            throw new FaceVerificationException(
                    "Face verification service is unavailable. Please try again.");
        }

        log.info(
                "Anti-spoof response for userId {}: {}",
                userId,
                antiSpoofResponse);

        if (antiSpoofResponse == null
                || antiSpoofResponse.isEmpty()) {

            throw new FaceVerificationException(
                    "Invalid anti-spoof response");
        }


        // ========================================================
        // 4. ANTI-SPOOF SUCCESS
        // ========================================================

        boolean antiSpoofSuccess =
                getBoolean(
                        antiSpoofResponse,
                        "success");

        if (!antiSpoofSuccess) {

            throw new FaceVerificationException(
                    "Anti-spoofing failed: "
                            + getString(
                                    antiSpoofResponse,
                                    "message",
                                    "Anti-spoofing failed"));
        }


        // ========================================================
        // 5. FACE DETECTED
        // ========================================================

        boolean faceDetected =
                getBoolean(
                        antiSpoofResponse,
                        "faceDetected");

        if (!faceDetected) {

            throw new FaceVerificationException(
                    "No face detected. Please position your face inside the camera.");
        }


        // ========================================================
        // 6. FACE COUNT
        // ========================================================

        Integer faceCount =
                getInteger(
                        antiSpoofResponse,
                        "faceCount");

        if (faceCount == null) {

            throw new FaceVerificationException(
                    "Unable to determine face count. Please try again.");
        }

        if (faceCount == 0) {

            throw new FaceVerificationException(
                    "No face detected. Please position your face inside the camera.");
        }

        if (faceCount > 1) {

            throw new FaceVerificationException(
                    "Multiple faces detected. Please ensure only one person is visible.");
        }


        // ========================================================
        // 7. LIVE CHECK
        // ========================================================

        boolean live =
                getBoolean(
                        antiSpoofResponse,
                        "live");

        if (!live) {

            throw new FaceVerificationException(
                    "Spoof detected. Please use a live face.");
        }


        // ========================================================
        // 8. VERIFY FACE EMBEDDING
        // ========================================================

        Map<String, Object> response;

        try {

            response =
                    faceRecognitionClient.verifyEmbedding(
                            registeredEmbedding,
                            file);

        } catch (Exception ex) {

            log.error(
                    "Face matching service failed for userId {}",
                    userId,
                    ex);

            throw new FaceVerificationException(
                    "Face verification service is unavailable. Please try again.");
        }

        log.info(
                "Face verification response for userId {}: {}",
                userId,
                response);

        if (response == null
                || response.isEmpty()) {

            throw new FaceVerificationException(
                    "Invalid face verification response");
        }


        // ========================================================
        // 9. VERIFIED
        // ========================================================

        boolean verified =
                getBoolean(
                        response,
                        "verified");


        // ========================================================
        // 10. DISTANCE
        // ========================================================

        Double distance =
                getDouble(
                        response,
                        "distance");

        if (distance == null) {

            throw new FaceVerificationException(
                    "Invalid face verification response: distance missing");
        }


        // ========================================================
        // 11. THRESHOLD
        // ========================================================

        Double threshold =
                getDouble(
                        response,
                        "threshold");

        if (threshold == null) {

            throw new FaceVerificationException(
                    "Invalid face verification response: threshold missing");
        }

        if (threshold <= 0) {

            throw new FaceVerificationException(
                    "Invalid face verification threshold");
        }


        // ========================================================
        // 12. FACE MATCH FAILED
        // ========================================================

        if (!verified) {

            String message =
                    getString(
                            response,
                            "message",
                            "Face verification failed. Please try again.");

            throw new FaceVerificationException(
                    message);
        }


        // ========================================================
        // 13. MODEL
        // ========================================================

        String model =
                getString(
                        response,
                        "model",
                        null);


        // ========================================================
        // 14. MESSAGE
        // ========================================================

        String message =
                getString(
                        response,
                        "message",
                        "Same person");


        // ========================================================
        // FINAL RESULT
        // ========================================================

        return FaceVerificationResult.builder()

                .userId(userId)

                .verified(true)

                .distance(distance)

                .threshold(threshold)

                .model(model)

                .message(message)

                .build();
    }


    // ============================================================
    // ACTIVE USER VALIDATION
    // ============================================================

    private void validateActiveUser(User user) {

        if (user == null) {

            throw new ResourceNotFoundException(
                    "User not found");
        }

        if (user.getStatus() == null) {

            throw new AttendanceException(
                    "User status is not available");
        }

        if (!"ACTIVE".equalsIgnoreCase(
                user.getStatus().name())) {

            throw new AttendanceException(
                    "User is inactive");
        }
    }


    // ============================================================
    // CONFIDENCE
    // ============================================================

    private Double calculateConfidence(
            FaceVerificationResult result) {

        if (result == null
                || !result.isVerified()) {

            return 0.0;
        }

        double distance =
                result.getDistance();

        double threshold =
                result.getThreshold();

        if (threshold <= 0) {

            return null;
        }

        double confidence =
                (1.0 - (distance / threshold))
                        * 100.0;

        return Math.max(
                0.0,
                Math.min(
                        100.0,
                        confidence));
    }


    // ============================================================
    // RESPONSE BUILDER
    // ============================================================

    private AttendanceMarkResponse buildAttendanceResponse(

            Attendance attendance,

            String action,

            String message,

            FaceVerificationResult faceResult,

            Double latitude,

            Double longitude

    ) {

        if (attendance == null) {

            throw new AttendanceException(
                    "Unable to build attendance response");
        }

        return AttendanceMarkResponse.builder()

                .success(true)

                .action(action)

                .message(message)

                .userId(
                        attendance.getUser().getId())

                .username(
                        attendance.getUser().getUsername())

                .attendanceDate(
                        attendance.getAttendanceDate())

                .checkInTime(
                        attendance.getCheckInTime())

                .checkOutTime(
                        attendance.getCheckOutTime())

                .status(
                        attendance.getStatus())

                .confidence(
                        attendance.getConfidence())

                .distance(
                        faceResult != null
                                ? faceResult.getDistance()
                                : null)

                .threshold(
                        faceResult != null
                                ? faceResult.getThreshold()
                                : null)

                .latitude(latitude)

                .longitude(longitude)

                .source(
                        attendance.getSource())

                .build();
    }


    // ============================================================
    // USER - MY ATTENDANCE
    // ============================================================

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance() {

        User user =
                getLoggedInUser();

        return attendanceRepository
                .findByUserIdOrderByAttendanceDateDesc(
                        user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // USER - MY ATTENDANCE BY DATE
    // ============================================================

    @Transactional(readOnly = true)
    public AttendanceResponse getMyAttendanceByDate(
            LocalDate date) {

        if (date == null) {

            throw new AttendanceException(
                    "Attendance date is required");
        }

        User user =
                getLoggedInUser();

        return attendanceRepository
                .findByUserIdAndAttendanceDate(
                        user.getId(),
                        date)
                .map(this::toResponse)
                .orElse(null);
    }


    // ============================================================
    // GET LOGGED-IN USER
    // ============================================================

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new AttendanceException(
                    "User is not authenticated");
        }

        String username =
                authentication.getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Logged-in user not found"));
    }


    // ============================================================
    // FACE FILE VALIDATION
    // ============================================================

    private void validateFaceFile(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            throw new FaceVerificationException(
                    "Face image is required");
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !contentType
                        .toLowerCase()
                        .startsWith("image/")) {

            throw new FaceVerificationException(
                    "Only image files are allowed");
        }
    }


    // ============================================================
    // LOCATION VALIDATION / COLLEGE GEOFENCE
    // ============================================================

    private void validateLocation(

            Double latitude,

            Double longitude) {

        // --------------------------------------------------------
        // 1. Basic GPS validation
        // --------------------------------------------------------

        if (latitude == null
                || longitude == null) {

            throw new AttendanceException(
                    "Location is required");
        }

        if (latitude.isNaN()
                || latitude.isInfinite()
                || latitude < -90
                || latitude > 90) {

            throw new AttendanceException(
                    "Invalid latitude");
        }

        if (longitude.isNaN()
                || longitude.isInfinite()
                || longitude < -180
                || longitude > 180) {

            throw new AttendanceException(
                    "Invalid longitude");
        }


        // --------------------------------------------------------
        // 2. Calculate distance from college
        // --------------------------------------------------------

        double distanceMeters =
                calculateDistanceInMeters(
                        latitude,
                        longitude,
                        collegeLatitude,
                        collegeLongitude);


        log.info(
                "Attendance location validation: " +
                "userLatitude={}, userLongitude={}, " +
                "collegeLatitude={}, collegeLongitude={}, " +
                "distanceMeters={}, allowedRadiusMeters={}",
                latitude,
                longitude,
                collegeLatitude,
                collegeLongitude,
                distanceMeters,
                attendanceRadiusMeters);


        // --------------------------------------------------------
        // 3. College geofence validation
        // --------------------------------------------------------

        if (distanceMeters > attendanceRadiusMeters) {

            throw new AttendanceException(
                    String.format(
                            "You are outside the college attendance area. " +
                            "You are approximately %.0f meters away. " +
                            "Allowed radius is %.0f meters.",
                            distanceMeters,
                            attendanceRadiusMeters
                    )
            );
        }
    }


    // ============================================================
    // DISTANCE CALCULATION
    // ============================================================

    private double calculateDistanceInMeters(

            double latitude1,

            double longitude1,

            double latitude2,

            double longitude2) {

        final double EARTH_RADIUS_METERS =
                6371000.0;

        double latitude1Radians =
                Math.toRadians(latitude1);

        double latitude2Radians =
                Math.toRadians(latitude2);

        double deltaLatitude =
                Math.toRadians(
                        latitude2 - latitude1);

        double deltaLongitude =
                Math.toRadians(
                        longitude2 - longitude1);

        double a =
                Math.sin(deltaLatitude / 2)
                        * Math.sin(deltaLatitude / 2)
                        +
                        Math.cos(latitude1Radians)
                                * Math.cos(latitude2Radians)
                                * Math.sin(deltaLongitude / 2)
                                * Math.sin(deltaLongitude / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }


    // ============================================================
    // ALREADY MARKED RESPONSE
    // ============================================================

    private AttendanceMarkResponse buildAlreadyMarkedResponse(

            Attendance attendance,

            String action,

            String message) {

        if (attendance == null) {

            throw new AttendanceException(
                    "Attendance record is required");
        }

        return AttendanceMarkResponse.builder()

                .success(false)

                .action(action)

                .message(message)

                .userId(
                        attendance.getUser().getId())

                .username(
                        attendance.getUser().getUsername())

                .attendanceDate(
                        attendance.getAttendanceDate())

                .checkInTime(
                        attendance.getCheckInTime())

                .checkOutTime(
                        attendance.getCheckOutTime())

                .status(
                        attendance.getStatus())

                .confidence(
                        attendance.getConfidence())

                .latitude(
                        attendance.getCheckInLatitude())

                .longitude(
                        attendance.getCheckInLongitude())

                .source(
                        attendance.getSource())

                .build();
    }


    // ============================================================
    // ENTITY -> RESPONSE
    // ============================================================

    private AttendanceResponse toResponse(
            Attendance attendance) {

        if (attendance == null) {

            throw new AttendanceException(
                    "Attendance record is invalid");
        }

        User user =
                attendance.getUser();

        if (user == null) {

            throw new AttendanceException(
                    "Attendance user is missing");
        }

        return AttendanceResponse.builder()

                .id(
                        attendance.getId())

                .userId(
                        user.getId())

                .username(
                        user.getUsername())

                .attendanceDate(
                        attendance.getAttendanceDate())

                .checkInTime(
                        attendance.getCheckInTime())

                .checkOutTime(
                        attendance.getCheckOutTime())

                .status(
                        attendance.getStatus())

                .confidence(
                        attendance.getConfidence())

                .checkInLatitude(
                        attendance.getCheckInLatitude())

                .checkInLongitude(
                        attendance.getCheckInLongitude())

                .checkOutLatitude(
                        attendance.getCheckOutLatitude())

                .checkOutLongitude(
                        attendance.getCheckOutLongitude())

                .source(
                        attendance.getSource())

                .createdAt(
                        attendance.getCreatedAt())

                .build();
    }


    // ============================================================
    // MAP HELPERS
    // ============================================================

    private boolean getBoolean(

            Map<String, Object> map,

            String key) {

        if (map == null
                || key == null) {

            return false;
        }

        Object value =
                map.get(key);

        if (value instanceof Boolean) {

            return (Boolean) value;
        }

        if (value instanceof String) {

            return Boolean.parseBoolean(
                    ((String) value).trim());
        }

        if (value instanceof Number) {

            return ((Number) value).doubleValue() != 0;
        }

        return false;
    }


    private Double getDouble(

            Map<String, Object> map,

            String key) {

        if (map == null
                || key == null) {

            return null;
        }

        Object value =
                map.get(key);

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }

        if (value instanceof String) {

            try {

                return Double.parseDouble(
                        ((String) value).trim());

            } catch (NumberFormatException ignored) {

                return null;
            }
        }

        return null;
    }


    private Integer getInteger(

            Map<String, Object> map,

            String key) {

        if (map == null
                || key == null) {

            return null;
        }

        Object value =
                map.get(key);

        if (value instanceof Number) {

            return ((Number) value)
                    .intValue();
        }

        if (value instanceof String) {

            try {

                return Integer.parseInt(
                        ((String) value).trim());

            } catch (NumberFormatException ignored) {

                return null;
            }
        }

        return null;
    }


    private String getString(

            Map<String, Object> map,

            String key,

            String defaultValue) {

        if (map == null
                || key == null) {

            return defaultValue;
        }

        Object value =
                map.get(key);

        if (value == null) {

            return defaultValue;
        }

        String result =
                String.valueOf(value).trim();

        return result.isEmpty()
                ? defaultValue
                : result;
    }
}
