# Partners Portal

Java
Spring Boot
React
TypeScript
PostgreSQL
Docker
MinIO

Total control over compliance and facilities in B2B contractual relationships.
Partners Portal is a modern web application designed to streamline document management in B2B contractual relationships, replacing slow and error-prone email exchanges with a secure, centralized portal.

## The Problem It Solves

In traditional B2B relationships, service providers must constantly send legal compliance documents (company certifications, employee records, salary statements, FGTS, ASO, etc.) via email to the contracting company. This process is:

Time-consuming
Prone to lost or outdated files
Difficult to track approval status
Inefficient for large teams

Partners Portal eliminates these pain points by providing a dedicated platform where contracted companies upload documents, and the contracting company reviews, approves, or rejects them — all with real-time visual feedback.

## Key Features

Visual Feedback Dashboard – Traffic light system (green = approved, yellow = pending, red = rejected) for company and employee documents
Secure Document Upload – Drag & drop interface for company-wide and per-employee files
Employee Management – Add, edit, and remove employees with dedicated document tracking
Invitation Links – Contracting companies generate secure access links for service providers
Real-time Status Updates – Instant visibility of approved/rejected documents
Centralized Storage – All documents stored securely with audit trail
Notification System – Alerts for new uploads and status changes (planned)

## Target Audience

Contracting Companies – Large organizations managing multiple service providers (compliance, HR, facilities teams)
Service Providers – Companies delivering outsourced services requiring regular document validation

## Tech Stack

BackendJava 21 + Spring Boot 3.3
FrontendReact 18 + TypeScript + Material UI
DatabasePostgreSQL 16
Object StorageMinIO (S3-compatible)
AuthJWT (Access + Refresh Tokens)
ContainerizationDocker + Docker Compose

Highlights:

Clean architecture with reusable components
Full containerization (one command to run everything)
Production-ready cloud deployment potential
Scalable and maintainable codebase
