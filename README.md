# Paladin - AI-Powered Job Application Assistant

A comprehensive backend system that streamlines job applications by leveraging AI to analyze CVs, generate tailored cover letters, and manage job application workflows with automated email submission.

## Features

### Core Functionality
- **OAuth2 Authentication** - Secure Google OAuth2 integration with token management
- **Profile Management** - Create and manage multiple professional profiles with CVs
- **CV Storage** - Secure S3-based CV storage with upload, download, and deletion
- **AI Job Analysis** - Claude AI integration for intelligent CV-job description matching
- **Smart Cover Letter Generation** - AI-powered personalized cover letters
- **Automated Email Sending** - Direct job application submission via Gmail API
- **Application Tracking** - Track application status (SENT, INTERVIEW, REJECTED, ACCEPTED, FOLLOW_UP)

### Technical Highlights
- **Spring Boot 3.x** with modern Java practices
- **JPA/Hibernate** for robust data management
- **AWS S3** integration for file storage
- **Anthropic Claude AI** for intelligent analysis
- **Gmail API** for automated email sending
- **Scheduled token refresh** to maintain OAuth2 sessions
- **RESTful API** design with comprehensive error handling

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (or your preferred database)
- AWS Account (for S3)
- Google Cloud Console project (for OAuth2 and Gmail API)
- Anthropic API key (for Claude AI)

## ️ Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/oluwatimilehinawoniyi/paladin-be.git
cd paladin-be
```

### 2. Configure Environment Variables

Create `application.yml` in `src/main/resources/`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/paladin_db
    username: your_db_username
    password: your_db_password
    
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/oauth2/callback/google"
            scope:
              - email
              - profile
              - https://www.googleapis.com/auth/gmail.send

  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-5-20250929

aws:
  access-key-id: ${AWS_ACCESS_KEY}
  secret-access-key: ${AWS_SECRET_KEY}
  s3:
    bucket-name: ${AWS_S3_BUCKET}
    region: ${AWS_REGION}

app:
  frontend:
    url: http://localhost:5173
```

### 3. Set Up Google OAuth2

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API and Gmail API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs:
    - `http://localhost:8080/oauth2/callback/google`
    - Your production URL

### 4. Set Up AWS S3

1. Create an S3 bucket
2. Configure CORS policy:
```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
    "AllowedOrigins": ["*"],
    "ExposeHeaders": []
  }
]
```

### 5. Build & Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## API Documentation

### Authentication Endpoints

#### Login via Google
```http
GET /oauth2/authorization/google
```

#### Get Current User
```http
GET /api/auth/me
```

#### Logout
```http
POST /api/auth/logout
```

### Profile Management

#### Create Profile with CV
```http
POST /api/v1/profiles
Content-Type: multipart/form-data

{
  "title": "Senior Software Engineer",
  "summary": "Experienced developer...",
  "skills": ["Java", "Spring Boot", "AWS"],
  "file": <CV_FILE>
}
```

#### Get User's Profiles
```http
GET /api/v1/profiles/me
```

#### Get Profile by ID
```http
GET /api/v1/profiles/{profileId}
```

#### Update Profile
```http
PATCH /api/v1/profiles/{profileId}
Content-Type: application/json

{
  "title": "Updated Title",
  "summary": "Updated summary",
  "skills": ["New", "Skills"]
}
```

#### Delete Profile
```http
DELETE /api/v1/profiles/{profileId}
```

### CV Management

#### Upload CV
```http
POST /api/v1/cv/upload
Content-Type: multipart/form-data

{
  "file": <CV_FILE>,
  "profileId": "uuid"
}
```

#### Download CV
```http
GET /api/v1/cv/{cvId}/download
```

#### Update CV
```http
PUT /api/v1/cv/{cvId}
Content-Type: multipart/form-data

{
  "file": <NEW_CV_FILE>
}
```

#### Delete CV
```http
DELETE /api/v1/cv/{cvId}
```

### Job Application

#### Analyze Job Application (AI-Powered)
```http
POST /api/v1/job-applications/analyze-application
Content-Type: application/json

{
  "profileId": "uuid",
  "jobDescription": "Full job posting text..."
}
```

**Response:**
```json
{
  "jobDetails": {
    "company": "Tech Corp",
    "position": "Senior Developer",
    "jobEmail": "hr@techcorp.com"
  },
  "coverLetter": "Dear Hiring Manager...",
  "matchAnalysis": {
    "overallMatchPercentage": 85,
    "strengths": ["Strong backend experience", "AWS expertise"],
    "gaps": ["Limited frontend experience"],
    "recommendation": "Highly recommended to apply",
    "confidenceLevel": "High"
  }
}
```

#### Send Job Application
```http
POST /api/v1/job-applications/send
Content-Type: application/json

{
  "profileId": "uuid",
  "company": "Tech Corp",
  "jobTitle": "Senior Developer",
  "jobEmail": "hr@techcorp.com",
  "subject": "Application for Senior Developer",
  "bodyText": "Cover letter content..."
}
```

#### Get My Applications
```http
GET /api/v1/job-applications/me
```

#### Update Application Status
```http
PATCH /api/v1/job-applications/{applicationId}/status
Content-Type: application/json

"INTERVIEW"
```

### Cover Letter Generation

#### Generate Cover Letter Template
```http
GET /api/v1/cover-letters/generate/{category}?candidateName=John&companyName=TechCorp&position=Developer

Categories: professional, enthusiastic, results, conversational
```

### User Management

#### Get Current User Details
```http
GET /api/v1/users/me
```

#### Delete Account
```http
DELETE /api/v1/users/me
```
⚠️ This permanently deletes all user data including profiles, CVs, and applications

## Architecture

```
┌─────────────────┐
│   Frontend      │
│   (SvelteKit)   │
└────────┬────────┘
         │
         │ REST API
         │
┌────────▼───────────────────────────────────┐
│          Spring Boot Backend               │
│                                            │
│  ┌──────────────┐  ┌──────────────┐        │
│  │ Controllers  │  │   Services   │        │
│  └──────┬───────┘  └──────┬───────┘        │
│         │                 │                │
│  ┌──────▼─────────────────▼───────┐        │
│  │      Business Logic            │        │
│  │  - OAuth2 Management           │        │
│  │  - File Upload/Download        │        │
│  │  - AI Analysis                 │        │
│  │  - Email Automation            │        │
│  └──────┬─────────────────────────┘        │
│         │                                  │
│  ┌──────▼──────────┐  ┌─────────────┐      │
│  │   Repositories  │  │   Mappers   │      │
│  └──────┬──────────┘  └─────────────┘      │
└─────────┼──────────────────────────────────┘
          │
    ┌─────┴─────────────────────────┐
    │                               │
┌───▼────┐  ┌──────┐  ┌──────────┐ ┌▼─────┐
│   DB   │  │  S3  │  │ Gmail API│ │Claude│
└────────┘  └──────┘  └──────────┘ └──────┘
```

## Security Features

- **OAuth2 Authentication** with Google
- **Session Management** with automatic token refresh
- **User Authorization** - Users can only access their own data
- **Secure File Storage** in AWS S3
- **CORS Configuration** for frontend integration
- **CSRF Protection** disabled for API-only backend
- **HTTPS Strict Transport Security** headers

[//]: # ()
[//]: # (## Testing)

[//]: # ()
[//]: # (```bash)

[//]: # (# Run all tests)

[//]: # (mvn test)

[//]: # ()
[//]: # (# Run with coverage)

[//]: # (mvn test jacoco:report)

[//]: # (```)

## Database Schema

### Users
- OAuth2 user information
- Token management (access token, refresh token, expiry)
- Relationship to profiles

### Profiles
- Multiple profiles per user
- Title, summary, skills
- One-to-one relationship with CV

### CVs
- Stored in S3
- Metadata in database (filename, URL, size, content type)

### Job Applications
- Track all applications
- Status management
- Linked to profiles

## Deployment

### Docker Deployment

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build
docker build -t paladin-be .

# Run
docker run -p 8080:8080 \
  -e GOOGLE_CLIENT_ID=your_client_id \
  -e GOOGLE_CLIENT_SECRET=your_secret \
  -e AWS_ACCESS_KEY=your_key \
  -e AWS_SECRET_KEY=your_secret \
  -e ANTHROPIC_API_KEY=your_key \
  paladin-backend
```

[//]: # (### Production Considerations)

[//]: # ()
[//]: # (1. **Environment Variables** - Use secrets management &#40;AWS Secrets Manager, HashiCorp Vault&#41;)

[//]: # (2. **Database** - Use managed database service &#40;RDS, Cloud SQL&#41;)

[//]: # (3. **File Storage** - Configure S3 bucket policies and lifecycle rules)

[//]: # (4. **Monitoring** - Add application monitoring &#40;CloudWatch, Datadog&#41;)

[//]: # (5. **Logging** - Centralized logging solution)

[//]: # (6. **Rate Limiting** - Implement API rate limiting)

[//]: # (7. **Backup Strategy** - Regular database and S3 backups)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

[//]: # (## License)

[//]: # (This project is licensed under the MIT License - see the LICENSE file for details)

## Author

Your Name - [Oluwatimilehin Awoniyi](https://www.linkedin.com/in/oluwatimilehin-awoniyi/)

[//]: # (Project Link: [https://github.com/yourusername/paladin]&#40;https://github.com/yourusername/paladin&#41;)
