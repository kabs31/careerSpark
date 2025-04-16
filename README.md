# CareerSpark Job Application System

An AI-powered job application automation system with Spring Boot backend and Python FastAPI AI service.

## System Architecture

The system consists of two main components:

1. **Spring Boot Backend** - Handles the core application logic, resume parsing, Selenium web automation, and job application management
2. **Python FastAPI AI Service** - Manages AI interactions using Ollama for analyzing forms and generating responses

## Setup Instructions

### Prerequisites

- Java 17+
- Maven
- Docker and Docker Compose
- Chrome browser (for Selenium)
- ChromeDriver (for Selenium)

### Running with Docker Compose

The easiest way to start both the AI service and Ollama:

```bash
docker-compose up -d
```

This will start:
- Ollama LLM server on port 11434
- AI Service on port 8000

### Running the Spring Boot Backend

```bash
cd careerspark-backend
mvn spring-boot:run
```

The Spring Boot application will start on port 8080.

### Running the AI Service Locally

```bash
cd ai_service
pip install -r requirements.txt
uvicorn main:app --reload
```

### System Flow

1. User uploads their resume to the system
2. User provides a job application URL
3. System extracts form fields from the job application page
4. AI generates responses based on the resume and job details
5. User reviews and optionally modifies the responses
6. System submits the application using Selenium

## API Documentation

### Spring Boot Backend API

- `GET /applications` - List all job applications
- `GET /applications/{id}` - Get application details
- `POST /applications/create` - Create a new job application
- `PUT /applications/{id}/fields` - Update form field values
- `POST /applications/{id}/submit` - Submit an application

### Python FastAPI AI Service

- `POST /analyze-form` - Analyze form structure
- `POST /generate-responses` - Generate responses for form fields
- `POST /analyze-page` - Determine if submission is complete or which button to click

## Key Components

### Spring Boot Services

- `FormFieldExtractorService` - Extracts form fields from job application pages using Selenium
- `AIServiceClient` - Communicates with the Python AI service
- `ApplicationFlowService` - Orchestrates the application process
- `SubmissionService` - Handles the form submission process

### Python AI Service

- Uses Ollama to generate responses based on resume data
- Analyzes page content to determine submission status
- Recommends which buttons to click for form submission

## Notes

- Make sure Ollama is running and has the required models loaded
- The system works best with standardized job application forms
- Application submission success depends on the job portal's structure and any anti-bot measures
