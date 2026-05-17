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

### 📥 Step 1: Clone Repository
```bash
git clone https://github.com/username/CanhBaoSuCo.git
cd CanhBaoSuCo
🐳 Step 2: Run Database (Docker)
docker-compose up -d
MySQL default port: 8081
Can be changed in docker-compose.yml
🚀 Step 3: Run Backend Server
cd suco
mvn spring-boot:run

Backend runs at:

http://localhost:8080
📱 Step 4: Run Android Client (Optional)
Open CanhBao in Android Studio
Configure backend IP address
Run on emulator or real device
🧠 System Highlights (CV / Interview)
Designed scalable RESTful backend for real-time incident management
Implemented geo-based filtering for proximity detection
Built emergency SOS routing logic for nearest rescue coordination
Integrated Google OAuth2 authentication system
Designed relational database schema for multi-entity system
Containerized backend and database using Docker
🧰 Project Structure
CanhBaoSuCo/
│
├── suco/                  # Spring Boot Backend
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── config/
│   ├── security/
│   ├── model/
│   └── dto/
│
├── docker-compose.yml     # Infrastructure setup
└── README.md
📧 Contact

Author: Dat Phan
Email: your-email@example.com

Position: Backend Java Spring Intern Candidate
