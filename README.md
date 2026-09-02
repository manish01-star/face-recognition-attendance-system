# 🎓 Face Recognition Attendance System

> **A secure, intelligent and automated college attendance management system powered by Face Recognition and Anti-Spoofing technology.**

A full-stack attendance management platform designed to automate student and teacher attendance using **AI-powered face recognition**, **anti-spoofing mechanisms**, and a modern distributed architecture.

The system combines an **Android mobile application**, **Spring Boot backend**, and a dedicated **Python FastAPI face recognition service** to provide a secure, scalable and efficient attendance workflow.

---

## 🚀 Project Overview

Traditional attendance systems are often time-consuming, manual, and vulnerable to proxy attendance.

The **Face Recognition Attendance System** solves these problems by providing an automated biometric attendance solution where registered students and teachers can be identified using their facial features.

The system performs:

**Face Capture → Face Detection → Face Recognition → Anti-Spoofing Verification → Attendance Validation → Database Recording**

This ensures that attendance is recorded only after successful identity verification and liveness/security checks.

---

## ✨ Key Features

### 🔐 Secure Authentication

* Admin-based authentication
* JWT-based authorization
* Role-based access control
* Secure password handling
* Protected REST APIs
* Session/token validation

### 🤖 AI-Powered Face Recognition

* Real-time face detection
* Facial feature extraction
* Face embedding comparison
* Similarity-based identity verification
* Configurable face matching threshold
* Support for both students and teachers

### 🛡️ Anti-Spoofing

The system is designed to reduce fraudulent attendance attempts using images or unauthorized representations.

Security mechanisms include:

* Face verification
* Liveness/anti-spoofing validation
* Image-based spoof detection
* Face similarity threshold validation
* Controlled face enrollment
* Attendance verification before recording

### 📱 Android Application

* Modern Android-based user interface
* Camera integration
* Face capture
* Attendance verification
* Location-aware attendance workflow
* API-based communication with backend services
* Secure authentication flow

### ⚙️ Spring Boot Backend

The backend acts as the central application layer responsible for:

* User management
* Student management
* Teacher management
* Attendance management
* Authentication and authorization
* Database operations
* Face service integration
* API validation
* Business logic

### 🧠 Dedicated Face Recognition Service

A separate Python service handles AI/ML operations.

Built using:

* Python
* FastAPI
* DeepFace
* TensorFlow
* OpenCV
* NumPy

This separation keeps AI processing independent from the main business application.

### 🗄️ Attendance Management

The system maintains attendance records for:

* Students
* Teachers
* Attendance date
* Attendance time
* Verification status
* Identity information
* Attendance history

### 👨‍💼 Admin Management

The administrator can:

* Register students
* Register teachers
* Create user accounts
* Capture face data
* Manage users
* View attendance
* Monitor attendance records
* Control system access

---

# 🏗️ System Architecture

```text
                         ┌──────────────────────┐
                         │     Android App      │
                         │      (Client)        │
                         └──────────┬───────────┘
                                    │
                                    │ REST API
                                    ▼
                         ┌──────────────────────┐
                         │    Spring Boot API   │
                         │   Business Backend   │
                         └──────────┬───────────┘
                                    │
                       ┌────────────┴────────────┐
                       │                         │
                       ▼                         ▼
              ┌─────────────────┐       ┌──────────────────┐
              │      MySQL      │       │  FastAPI Face    │
              │    Database     │       │ Recognition      │
              └─────────────────┘       │    Service       │
                                        └────────┬─────────┘
                                                 │
                                    ┌────────────┴────────────┐
                                    │                         │
                                    ▼                         ▼
                              Face Recognition          Anti-Spoofing
                              / Verification             Validation
```

---

# 🔄 Attendance Workflow

```text
User
  │
  ▼
Open Attendance
  │
  ▼
Camera Capture
  │
  ▼
Face Detection
  │
  ▼
Anti-Spoofing Check
  │
  ├── Failed ──────► Attendance Rejected
  │
  ▼
Face Recognition
  │
  ├── Not Matched ─► Attendance Rejected
  │
  ▼
Identity Verification
  │
  ▼
Attendance Validation
  │
  ▼
Store Attendance
  │
  ▼
Attendance Successfully Recorded
```

---

# 🧩 Main Modules

## 1. Authentication Module

Handles:

* Admin login
* User authentication
* JWT token generation
* Token validation
* Authorization

---

## 2. Student Management

Admin can:

* Add students
* Update student information
* Register face data
* Activate/deactivate accounts
* View student details

---

## 3. Teacher Management

Admin can:

* Add teachers
* Update teacher information
* Register teacher face data
* Manage teacher accounts
* Activate/deactivate accounts

---

## 4. Face Recognition Module

Responsible for:

* Face detection
* Face preprocessing
* Feature extraction
* Face embedding generation
* Face comparison
* Similarity calculation
* Identity verification

---

## 5. Anti-Spoofing Module

Provides additional protection against:

* Printed photographs
* Images displayed on mobile screens
* Unauthorized face representations
* Basic presentation attacks

---

## 6. Attendance Module

Handles:

* Attendance verification
* Attendance creation
* Attendance date/time
* Attendance history
* Student attendance
* Teacher attendance
* Duplicate attendance prevention

---

## 7. Admin Module

Provides centralized control over the system.

Admin can manage:

* Students
* Teachers
* Face registrations
* Attendance records
* User accounts
* System operations

---

# 🛠️ Technology Stack

| Layer              | Technology        |
| ------------------ | ----------------- |
| Mobile Application | Android           |
| Backend            | Java, Spring Boot |
| AI/ML Service      | Python, FastAPI   |
| Face Recognition   | DeepFace          |
| Machine Learning   | TensorFlow        |
| Computer Vision    | OpenCV            |
| Database           | MySQL             |
| API Communication  | REST API          |
| Authentication     | JWT               |
| Build Tool         | Maven / Gradle    |
| Version Control    | Git & GitHub      |
| API Documentation  | Swagger / OpenAPI |

---

# 📁 Project Structure

```text
face-recognition-attendance-system/
│
├── attendance-backend/
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── attendance-face-service/
│   ├── app/
│   │   ├── api/
│   │   ├── core/
│   │   ├── schemas/
│   │   ├── services/
│   │   ├── main.py
│   │   └── __init__.py
│   │
│   ├── requirements.txt
│   └── README.md
│
├── attendance-mobile/
│   ├── app/
│   ├── gradle/
│   └── README.md
│
├── .gitignore
└── README.md
```

---

# 🔒 Security

Security is an important part of the system architecture.

The application implements multiple layers of protection:

* JWT authentication
* API authorization
* Password encryption
* Face verification
* Anti-spoofing validation
* Configurable face similarity threshold
* Input validation
* Database constraints
* Controlled user registration
* Environment-based configuration
* Sensitive configuration exclusion through `.gitignore`

> **Important:** Production deployments should use environment variables or secure secret management for database credentials, JWT secrets, API keys and other sensitive configuration.

---

# 🧠 Face Recognition Pipeline

```text
Camera Image
     │
     ▼
Image Preprocessing
     │
     ▼
Face Detection
     │
     ▼
Face Alignment
     │
     ▼
Feature Extraction
     │
     ▼
Face Embedding
     │
     ▼
Embedding Comparison
     │
     ▼
Similarity Score
     │
     ▼
Threshold Validation
     │
     ├── Match ─────► Identity Verified
     │
     └── No Match ──► Verification Failed
```

---

# 📊 Attendance Verification

Attendance is recorded only when the required verification conditions are successfully satisfied.

```text
Face Detected
      +
Anti-Spoofing Passed
      +
Face Match Successful
      +
User Valid
      ↓
Attendance Recorded
```

This approach helps reduce unauthorized or fraudulent attendance attempts.

---

# ⚡ Backend Architecture

The Spring Boot application follows a layered architecture:

```text
Controller
    │
    ▼
Service
    │
    ▼
Repository / DAO
    │
    ▼
MySQL Database
```

The backend is responsible for business rules while the Python service handles computationally intensive face recognition operations.

---

# 🔗 Service Communication

The Spring Boot backend communicates with the FastAPI face recognition service through REST APIs.

```text
Android
   │
   ▼
Spring Boot
   │
   │ Face Verification Request
   ▼
FastAPI
   │
   ▼
DeepFace / OpenCV
   │
   ▼
Recognition Result
   │
   ▼
Spring Boot
   │
   ▼
Attendance Decision
```

---

# 💻 Installation & Setup

## Prerequisites

Make sure the following are installed:

* Java 17+
* Maven
* Python 3.10+
* MySQL
* Android Studio
* Git

---

## 1️⃣ Clone Repository

```bash
git clone https://github.com/manish01-star/face-recognition-attendance-system.git
```

```bash
cd face-recognition-attendance-system
```

---

# 2️⃣ Backend Setup

Navigate to:

```bash
cd attendance-backend
```

Configure your database and application environment.

Then run:

```bash
mvn clean install
```

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

---

# 3️⃣ Face Recognition Service Setup

Navigate to:

```bash
cd attendance-face-service
```

Create a virtual environment:

```bash
python -m venv venv
```

Activate it on Windows:

```bash
venv\Scripts\activate
```

Install dependencies:

```bash
pip install -r requirements.txt
```

Start the FastAPI service:

```bash
uvicorn app.main:app --reload
```

The API documentation will be available through the FastAPI Swagger interface.

---

# 4️⃣ Android Application Setup

Open:

```text
attendance-mobile/
```

using **Android Studio**.

Configure the backend API URL according to your development environment.

Then:

```text
Sync Gradle
      ↓
Build Project
      ↓
Run Application
```

---

# 🗃️ Database

The application uses **MySQL** as the primary relational database.

The database stores information such as:

* Users
* Students
* Teachers
* Face registration data
* Attendance records
* Account status
* Authentication-related information

Database configuration should be maintained outside the source code in production environments.

---

# 📡 API Documentation

The backend APIs can be documented and tested using:

**Swagger / OpenAPI**

The FastAPI face recognition service also provides interactive API documentation for development and testing.

---

# 🎯 Project Objectives

The major objectives of this project are:

* Automate college attendance
* Reduce manual attendance work
* Minimize proxy attendance
* Provide biometric identity verification
* Introduce anti-spoofing protection
* Centralize attendance records
* Provide a scalable backend architecture
* Separate AI processing from business logic
* Provide a mobile-friendly attendance experience

---

# 🚀 Future Enhancements

Planned improvements may include:

* Advanced real-time liveness detection
* Improved presentation attack detection
* Attendance analytics dashboard
* Monthly and semester-wise reports
* Export attendance to Excel/PDF
* Email/SMS notifications
* Advanced role-based access control
* Cloud deployment
* Docker containerization
* CI/CD pipeline
* Face recognition performance optimization
* Detailed audit logging
* Admin analytics dashboard

---

# 📈 Scalability

The architecture is designed with separation of concerns in mind.

The independent face recognition service allows AI workloads to be scaled separately from the main Spring Boot application.

```text
                   Load Balancer
                        │
             ┌──────────┴──────────┐
             ▼                     ▼
       Spring Boot #1        Spring Boot #2
             │                     │
             └──────────┬──────────┘
                        │
                        ▼
                  MySQL Database

              Face Recognition Layer
                        │
             ┌──────────┴──────────┐
             ▼                     ▼
        FastAPI #1             FastAPI #2
```

This architecture can be extended for larger institutions and multi-campus deployments.

---

# 🧪 Testing

The system can be tested across multiple scenarios:

### Positive Cases

* Registered student face
* Registered teacher face
* Valid authentication
* Successful face verification

### Negative Cases

* Unknown face
* Incorrect user
* Spoofed image
* Invalid authentication token
* Duplicate attendance
* Inactive account

---

# 📌 Important Notes

This project is intended primarily as an **academic and portfolio project** demonstrating the integration of:

**Mobile Development + Backend Engineering + AI/ML + Computer Vision + Database Management + Security**

For production deployment, additional security hardening, privacy controls, consent mechanisms, biometric-data protection, monitoring, and more robust liveness detection should be implemented.

---

# 👨‍💻 Author

### Manish Thakur

**Software Developer | Java | Spring Boot | Python | Android | REST APIs**

Interested in building scalable backend systems, AI-integrated applications and modern software solutions.

---

# ⭐ Support

If you find this project useful or interesting, consider giving the repository a ⭐ on GitHub.

---

## 📜 License

This project is available for educational and portfolio purposes.
