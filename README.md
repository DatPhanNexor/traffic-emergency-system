# 🚨 Traffic Incident Warning & Emergency Response System (Backend Focus)

CanhBaoSuCo is a server-side platform designed to handle traffic incident reporting and emergency response coordination in real-time.

---

## 📌 Overview

The system focuses on:
- Processing user-reported traffic incidents  
- Providing geo-based incident filtering  
- Handling emergency SOS requests  
- Managing users, roles, and reward systems  
- Supporting mobile clients via REST APIs  

⚠️ Android application is only a client consuming backend APIs.

---

## 🚀 Key Backend Features

### 📍 Incident Management System
- Create, update, delete traffic incidents  
- Store incident types: accident, congestion, breakdown  
- REST APIs for full incident lifecycle management  

---

### 🌍 Geo-based Query System
- Retrieve incidents within a radius (R km)  
- Location-based filtering using latitude/longitude  
- Optimize user experience with nearby incident detection  

---

### 🆘 Emergency SOS System
- Handle real-time SOS requests from users  
- Route emergency signals to nearest support centers  
- Prioritize critical incidents  

---

### 👤 Authentication & Authorization
- Google OAuth2 login integration  
- Role-based access control (USER / ADMIN)  
- Secure API access using Spring Security  

---

### 🏆 Reward & Reporting System
- User-generated incident reporting  
- Basic reward/point system for contributions  
- Anti-spam validation for reports  

---

### 🏢 Admin Management APIs
- Manage incidents and user reports  
- Approve/reject reported incidents  
- System monitoring endpoints for admin dashboard  

---

## 🛠️ Tech Stack

### Backend Core
- Java 17  
- Spring Boot  
- Spring Security (OAuth2 Google Login)  
- Spring Data JPA  
- RESTful API architecture  

### Database
- MySQL  
- Relational schema design for users, incidents, reports, rewards  

### System Design
- Client–Server architecture  
- Geo-spatial query logic (radius-based filtering)  
- Role-based access control (RBAC)  

### DevOps
- Docker & Docker Compose  
- Maven build system  

### Client (Support Only)
- Android (Kotlin, Jetpack Compose)  
- Google Maps SDK  
> Used only as API consumer  

---

## 🏗️ System Architecture

The system follows a Client–Server architecture:


Android App (Client)
↓
REST API (Spring Boot Backend)
↓
Business Logic Layer
↓
MySQL Database


### Backend Responsibilities
- Business logic processing  
- Data validation & security  
- Geo-based computation  
- Incident & SOS handling  

### Client Responsibilities
- Displaying data  
- Sending user location  
- Triggering API requests  

---

## ⚙️ Installation Guide

### 🧩 Requirements
- Java 17+  
- Maven  
- Docker & Docker Compose  
- MySQL (or via Docker)  

---

# Traffic Emergency System

Traffic Emergency System is a real-time incident reporting and emergency support system.  
The project includes a Spring Boot backend, MySQL database, phpMyAdmin, Docker setup, and an optional Android client.

---

### 📥 Step 1: Clone Repository

```bash
git clone https://github.com/DatPhanNexor/traffic-emergency-system.git
cd traffic-emergency-system
```

---

### 🐳 Step 2: Run Database and phpMyAdmin (Docker)

Run the database and phpMyAdmin using Docker:

```bash
docker-compose up -d
```

phpMyAdmin runs at:

```txt
http://localhost:8081
```

MySQL runs at:

```txt
localhost:3307
```

These ports can be changed in:

```txt
docker-compose.yml
```

---

### 🚀 Step 3: Run Backend Server

Go to the backend project folder:

```bash
cd suco/suco
```

Run the Spring Boot backend server:

```bash
mvn spring-boot:run
```

Backend runs at:

```txt
http://localhost:8082
```

---

### 📱 Step 4: Run Android Client (Optional)

Open the Android client project in Android Studio.

```txt
CanhBao
```

Then follow these steps:

```txt
1. Configure the backend IP address.
2. Run the app on an emulator or a real Android device.
3. Make sure the backend server is running before testing the app.
```

---

### 🧠 System Highlights (CV / Interview)

```txt
- Designed a scalable RESTful backend for real-time incident management.
- Implemented geo-based filtering for proximity detection.
- Built emergency SOS routing logic for nearest rescue coordination.
- Integrated Google OAuth2 authentication system.
- Designed a relational database schema for a multi-entity system.
- Containerized the database and development environment using Docker.
```

---

### 🧰 Project Structure

```txt
traffic-emergency-system/
│
├── suco/
│   └── suco/                         # Spring Boot Backend
│       ├── src/
│       │   └── main/
│       │       ├── java/
│       │       │   └── com/example/suco/
│       │       │       ├── config/       # Security and application configuration
│       │       │       ├── controller/   # REST API controllers
│       │       │       ├── dto/          # Data transfer objects
│       │       │       ├── model/        # Entity models
│       │       │       ├── repository/   # Database repositories
│       │       │       ├── security/     # Authentication and authorization logic
│       │       │       └── service/      # Business logic services
│       │       │
│       │       └── resources/
│       │           └── application.properties
│       │
│       ├── pom.xml
│       └── mvnw.cmd
│
├── docker-compose.yml                # MySQL and phpMyAdmin setup
├── README.md
└── postman/                          # Postman collections and testing evidence
```

---

### 📧 Contact

```txt
Author: Dat Phan
Email: your-email@example.com
Position: Backend Java Spring Intern Candidate
```
