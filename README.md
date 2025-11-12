# 🏢 Asset Audit System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![H2 Database](https://img.shields.io/badge/Database-H2-blue.svg)](https://www.h2database.com/)
[![Docker](https://img.shields.io/badge/Docker-Supported-blue.svg)](https://www.docker.com/)
[![Railway](https://img.shields.io/badge/Deploy-Railway-purple.svg)](https://railway.app/)

A comprehensive **Asset Management and Audit System** powered by AI for automated asset recognition and tracking. This system enables organizations to efficiently manage their physical assets through photo capture, AI-powered analysis, and systematic audit workflows.

## 🌟 Features

### 🤖 AI-Powered Asset Recognition
- **Automated Information Extraction**: Uses OpenAI Vision API to extract device details from photos
- **Smart Asset Detection**: Automatically identifies device numbers, models, manufacturers, and serial numbers
- **Barcode Recognition**: Supports barcode scanning and recognition

### 📊 Comprehensive Asset Management
- **Asset Registration**: Create and manage asset records with detailed information
- **Location Tracking**: GPS coordinates and department-based asset location
- **Status Management**: Track asset conditions and audit status
- **Image Documentation**: Store and manage asset photos with organized file structure

### 🔍 Audit Workflow
- **Multi-Stage Audits**: Complete audit lifecycle from capture to completion
- **Auditor Assignment**: Track who performed each audit with timestamps
- **Evidence Collection**: Photo evidence with GPS coordinates
- **Condition Assessment**: Record asset condition and maintenance notes

### 🌐 Modern Web Interface
- **Responsive Dashboard**: Bootstrap-powered UI for desktop and mobile
- **Real-time Updates**: Live status updates and notifications
- **Gallery View**: Visual asset browser with thumbnail previews
- **Interactive Forms**: User-friendly asset capture and audit completion forms

### 🔧 Technical Features
- **RESTful APIs**: Complete REST API for mobile app integration
- **Database Management**: H2 database with JPA/Hibernate ORM
- **File Upload**: Secure file handling with organized storage
- **Docker Support**: Containerized deployment with multi-stage builds

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.3.4 with Java 17
- **Database**: H2 Database (file-based for persistence)
- **ORM**: Spring Data JPA with Hibernate
- **Template Engine**: Thymeleaf for server-side rendering
- **Build Tool**: Gradle 8.7.0
- **Containerization**: Docker with Eclipse Temurin JRE 17

### Project Structure
```
src/main/java/com/example/demo/
├── controller/          # Web Controllers
│   └── AssetAuditController.java
├── restController/      # REST API Controllers
│   ├── AssetRestController.java
│   └── AuditRestController.java
├── service/            # Business Logic Layer
│   ├── AssetService.java
│   ├── AuditService.java
│   ├── VisionAIService.java
│   └── ImageService.java
├── entity/             # JPA Entities
│   ├── Asset.java
│   └── Audit.java
├── repository/         # Data Access Layer
│   ├── AssetRepository.java
│   └── AuditRepository.java
├── dto/               # Data Transfer Objects
│   ├── AssetDTO.java
│   └── AuditDTO.java
└── mapper/            # MapStruct Mappers
    └── AssetMapper.java
```

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Docker (optional, for containerized deployment)
- OpenAI API Key (for AI features)

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd backend/demo
   ```

2. **Set environment variables**
   ```bash
   export OPENAI_API_KEY=your_openai_api_key_here
   ```

3. **Run with Gradle**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**
   - Web Interface: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - API Documentation: http://localhost:8080/api

### Docker Deployment

1. **Build and run with Docker**
   ```bash
   docker build -t asset-audit-system .
   docker run -p 8080:8080 -e OPENAI_API_KEY=your_key asset-audit-system
   ```

### Railway Deployment

1. **Connect your GitHub repository to Railway**
2. **Set environment variables in Railway dashboard:**
   - `OPENAI_API_KEY`: Your OpenAI API key
3. **Deploy using the included Dockerfile**

## 📖 API Documentation

### Asset Management APIs

#### Get All Assets
```http
GET /api/assets
```

#### Get Asset by Barcode
```http
GET /api/assets/{barcode}
```

### Audit Management APIs

#### Get All Audits
```http
GET /api/audit/all
```

#### Get Asset by Barcode (Audit Context)
```http
POST /api/audit/{barcode}
```

### Web Interface Routes

- `/` - Home/Gallery page
- `/audit` - Main audit dashboard
- `/audit/capture` - Asset capture form
- `/audit/assets` - Assets list view
- `/audit/audits` - Audits list view
- `/audit/asset/{id}` - Asset detail view
- `/audit/complete/{auditId}` - Complete audit form

## 🗄️ Database Configuration

### H2 Database Access
- **URL**: `jdbc:h2:file:./data/demo`
- **Username**: `sa`
- **Password**: `password`
- **Console**: http://localhost:8080/h2-console

### Entity Relationships
- **Asset** ↔ **Audit**: One-to-Many relationship
- Assets can have multiple audit records
- Each audit is linked to a specific asset

## 🔧 Configuration

### Application Properties
```properties
# Database Configuration
spring.datasource.url=jdbc:h2:file:./data/demo
spring.datasource.username=sa
spring.datasource.password=password

# H2 Console (Development)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=true

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload-dir=uploads/

# AI Configuration
vision.ai.api.key=${OPENAI_API_KEY:}
vision.ai.api.url=https://api.openai.com/v1/chat/completions

# Server Configuration
server.port=${PORT:8080}
```

## 🔒 Security Considerations

- **API Key Management**: OpenAI API key is managed through environment variables
- **File Upload Security**: File type validation and secure storage
- **Database Access**: H2 console should be disabled in production
- **Input Validation**: Comprehensive validation on all user inputs

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the H2 console for database-related issues
- Review application logs for troubleshooting

## 🔄 Version History

- **v0.0.1-SNAPSHOT**: Initial release with core asset management and AI integration features

---

**Built with ❤️ using Spring Boot and AI Technology**
