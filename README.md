### User Registration Flow
![image](https://github.com/user-attachments/assets/bdb4f08f-ea70-4ae3-a09a-7dd79a1d92ab)


# Booking Management System Application API Documentation
The API documentation for "A booking Management System beyond just "Events" 

## Table of Contents

1.  [Technologies Used](#1-technologies-used)
2.  [Getting Started](#2-getting-started)
    * [Prerequisites](#prerequisites)
    * [Local Setup](#local-setup)
3.  [Authentication & Authorization](#3-authentication--authorization)
    * [Roles](#roles)
    * [JWT Flow](#jwt-flow)
    * [API Key Configuration](#api-key-configuration)
4.  [API Endpoints](#4-api-endpoints)
    * [Authentication APIs](#authentication-apis)
    * [User APIs](#user-apis)
    * [Event APIs](#event-apis)
    * [Event Registration APIs](#event-registration-apis)
5.  [Data Models (DTOs)](#5-data-models-dtos)
6.  [Error Handling](#6-error-handling)

---

## 1. Technologies Used

* **Spring Boot:** Framework for building robust, production-ready Spring applications.
* **Spring Security:** Comprehensive security services for Java EE-based enterprise software applications.
* **Spring Data JPA / Hibernate:** For database interaction and ORM.
* **JWT (JSON Web Tokens):** For stateless authentication.
* **BCrypt:** Secure password hashing.
* **Lombok:** Reduces boilerplate code (getters, setters, constructors).
* **PostgreSQL (Example):** Relational database.
* **Maven:** Build automation tool.
* **Java 17+:** Programming language.

## 2. Getting Started

### Prerequisites

* Java Development Kit (JDK) 17 or higher
* Maven 3.6+
* A PostgreSQL database instance (or configure for another database)
* Postman or a similar API testing tool

### Local Setup

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url> 
    cd <folder-name>
    ```

2.  **Configure `application.properties` (or `application.yml`):**
    Create `src/main/resources/application.properties` and add your database and JWT configurations.

    ```properties
    # Database Configuration (Example for PostgreSQL)
    spring.datasource.url=jdbc:postgresql://localhost:5432/welcome_db
    spring.datasource.username=your_db_user
    spring.datasource.password=your_db_password
    spring.jpa.hibernate.ddl-auto=update # or create, create-drop
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true

    # JWT Security Configuration
    application.security.jwt.secret-key=YOUR_SUPER_SECRET_KEY_AT_LEAST_256_BITS_LONG_AND_BASE64_ENCODED_FOR_PRODUCTION # Replace with a strong, base64-encoded key
    application.security.jwt.expiration=900000  # 15 minutes in milliseconds
    application.security.jwt.refresh-token.expiration=604800000 # 7 days in milliseconds
    ```
    * **`secret-key` Note:** For production, generate a strong, random Base64-encoded key. You can generate one using `Base64.getEncoder().encodeToString(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded())` in Java.

3.  **Build the project:**
    ```bash
    mvn clean install
    ```

4.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080` (or your configured port).

## 3. Authentication & Authorization

The application uses **JWT (JSON Web Tokens)** for stateless authentication and **Role-Based Access Control (RBAC)** for authorization.

### Roles

* **`USER`**: Standard application user. Can register for events, view their own profile, and view public events.
* **`EVENT_ORGANIZER`**: Can create, update, and delete events they own. Can view registrations for their own events.
* **`ADMIN`**: Has full administrative privileges across all resources (users, events, registrations).

### JWT Flow

1.  **Login (`POST /api/v1/auth/login`):** Users provide username/password and receive an `accessToken` and `refreshToken`.
2.  **Access Protected Resources:** The `accessToken` is sent in the `Authorization: Bearer <token>` header for subsequent requests to protected endpoints.
3.  **Token Expiration:** The `accessToken` has a short lifespan.
4.  **Token Refresh (`POST /api/v1/auth/refresh`):** When the `accessToken` expires, the `refreshToken` can be used to obtain a new `accessToken` (and often a new `refreshToken` for rotation).

### API Key Configuration

For Postman, it's recommended to set up an environment variable, e.g., `baseUrl` with value `http://localhost:8080`.

## 4. API Endpoints

### Authentication APIs

#### **1. Register a New User**
* **Endpoint:** `POST /api/v1/user`
* **Purpose:** Creates a new user account.
* **Authentication:** Public (No token required).
* **Request Body:** `User` object (password will be BCrypt encoded).
    ```json
    {
        "username": "newuser",
        "email": "newuser@example.com",
        "password": "securepassword123",
        "roles": ["USER"]
    }
    ```
* **Success Response (201 Created):** `UserResponse` DTO.
    ```json
    {
        "id": 1,
        "username": "newuser",
        "email": "newuser@example.com",
        "roles": ["USER"]
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/user" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "newuser",
        "email": "newuser@example.com",
        "password": "securepassword123",
        "roles": ["USER"]
    }'
    ```

#### **2. User Login**
* **Endpoint:** `POST /api/v1/auth/login`
* **Purpose:** Authenticates a user and issues JWT access and refresh tokens.
* **Authentication:** Public.
* **Request Body:** `AuthRequest` DTO.
    ```json
    {
        "username": "testuser",
        "password": "password123"
    }
    ```
* **Success Response (200 OK):** `AuthResponse` DTO.
    ```json
    {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "testuser",
        "password": "password123"
    }'
    ```

#### **3. Refresh Access Token**
* **Endpoint:** `POST /api/v1/auth/refresh`
* **Purpose:** Obtains a new access token (and refresh token) using a valid refresh token.
* **Authentication:** Public (refresh token in body).
* **Request Body:** `RefreshTokenRequest` DTO.
    ```json
    {
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
    }
    ```
* **Success Response (200 OK):** `AuthResponse` DTO with new tokens.
    ```json
    {
        "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwb3N0bWFudXNlciIsImlhdCI6MTc1MjI5OTExNCwiZXhwIjoxNzUyMzg1NTE0fQ.lqUcYreONhOcHHXQdkH4VTGLS-eiNr7WeQv-w6i3B08",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwb3N0bWFudXNlciIsImlhdCI6MTc1MjI5OTExNCwiZXhwIjoxNzUyOTAzOTE0fQ.r_4MSJEYQ0vFir38TKiegZU-uxcwyrkboL_MYFr2ZuI"
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/auth/refresh" \
    -H "Content-Type: application/json" \
    -d '{
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
    }'
    ```

---

### User APIs

#### **1. Get Current Authenticated User's Profile**
* **Endpoint:** `GET /api/v1/user/me`
* **Purpose:** Retrieves the profile of the currently logged-in user.
* **Authentication:** Authenticated.
* **Success Response (200 OK):** `UserResponse` DTO.
    ```json
    {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        "roles": ["USER"]
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/user/me" \
    -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
    ```

#### **2. Get User by ID**
* **Endpoint:** `GET /api/v1/user/{id}`
* **Purpose:** Retrieves a user's profile by their ID.
* **Authentication:** `ADMIN` Role.
* **Success Response (200 OK):** `UserResponse` DTO.
    ```json
    {
        "id": 2,
        "username": "event_organizer",
        "email": "organizer@example.com",
        "roles": ["EVENT_ORGANIZER"]
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/user/2" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```

#### **3. Get All Users**
* **Endpoint:** `GET /api/v1/user`
* **Purpose:** Retrieves a list of all registered users.
* **Authentication:** `ADMIN` Role.
* **Success Response (200 OK):** List of `UserResponse` DTOs.
    ```json
    [
        { "id": 1, "username": "testuser", "email": "test@example.com", "roles": ["USER"] },
        { "id": 2, "username": "event_organizer", "email": "organizer@example.com", "roles": ["EVENT_ORGANIZER"] }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/user" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```

#### **4. Update User Profile**
* **Endpoint:** `PUT /api/v1/user/{id}`
* **Purpose:** Updates an existing user's profile.
* **Authentication:** User can update their own profile; `ADMIN` can update any profile.
* **Request Body:** `UserUpdateRequest` DTO.
    ```json
    {
        "username": "updated_testuser",
        "email": "updated_test@example.com",
        "roles": ["USER", "EVENT_ORGANIZER", "ADMIN"] 
    }
    ```
* **Success Response (200 OK):** Updated `UserResponse` DTO.
    ```json
    {
        "id": 1,
        "username": "updated_testuser",
        "email": "updated_test@example.com",
        "roles": ["USER"]
    }
    ```
* **Example (Postman/cURL - User updating self):**
    ```bash
    curl -X PUT "{{baseUrl}}/api/v1/user/1" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>" \
    -d '{
        "username": "my_new_username",
        "email": "my_new_email@example.com"
    }'
    ```

#### **5. Delete User**
* **Endpoint:** `DELETE /api/v1/user/{id}`
* **Purpose:** Deletes a user account.
* **Authentication:** `ADMIN` Role.
* **Success Response (204 No Content):** No body.
* **Example (Postman/cURL):**
    ```bash
    curl -X DELETE "{{baseUrl}}/api/v1/user/3" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```

---

### Event APIs

#### **1. Create an Event**
* **Endpoint:** `POST /api/v1/events`
* **Purpose:** Creates a new event. The logged-in user becomes the organizer.
* **Authentication:** `EVENT_ORGANIZER` or `ADMIN` Role.
* **Request Body:** `EventRequest` DTO.
    ```json
    {
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "eventDate": "2025-09-15T10:00:00",
        "location": "Online via Zoom"
    }
    ```
* **Success Response (201 Created):** `EventResponse` DTO.
    ```json
    {
        "id": 101,
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "eventDate": "2025-09-15T10:00:00",
        "location": "Online via Zoom",
        "organizerId": 2,
        "organizerUsername": "event_organizer",
        "createdAt": "2025-07-13T12:00:00",
        "updatedAt": "2025-07-13T12:00:00"
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/events" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_ACCESS_TOKEN>" \
    -d '{
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "eventDate": "2025-09-15T10:00:00",
        "location": "Online via Zoom"
    }'
    ```

#### **2. Get Event by ID**
* **Endpoint:** `GET /api/v1/events/{id}`
* **Purpose:** Retrieves details of a specific event.
* **Authentication:** Public.
* **Success Response (200 OK):** `EventResponse` DTO.
    ```json
    {
        "id": 101,
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "eventDate": "2025-09-15T10:00:00",
        "location": "Online via Zoom",
        "organizerId": 2,
        "organizerUsername": "event_organizer",
        "createdAt": "2025-07-13T12:00:00",
        "updatedAt": "2025-07-13T12:00:00"
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/events/101"
    ```

#### **3. Get All Events**
* **Endpoint:** `GET /api/v1/events`
* **Purpose:** Retrieves a list of all available events.
* **Authentication:** Public.
* **Success Response (200 OK):** List of `EventResponse` DTOs.
    ```json
    [
        {
            "id": 101,
            "name": "Spring Boot Workshop",
            "description": "An interactive workshop on Spring Boot basics.",
            "eventDate": "2025-09-15T10:00:00",
            "location": "Online via Zoom",
            "organizerId": 2,
            "organizerUsername": "event_organizer",
            "createdAt": "2025-07-13T12:00:00",
            "updatedAt": "2025-07-13T12:00:00"
        }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/events"
    ```

#### **4. Update Event**
* **Endpoint:** `PUT /api/v1/events/{id}`
* **Purpose:** Updates an existing event's details.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` who created the event.
* **Request Body:** `EventRequest` DTO.
    ```json
    {
        "name": "Updated Spring Boot Workshop",
        "description": "New description for the updated workshop.",
        "eventDate": "2025-09-20T14:00:00",
        "location": "Hybrid - Online & Venue"
    }
    ```
* **Success Response (200 OK):** Updated `EventResponse` DTO.
* **Example (Postman/cURL - Organizer updating their event):**
    ```bash
    curl -X PUT "{{baseUrl}}/api/v1/events/101" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_ACCESS_TOKEN>" \
    -d '{
        "name": "Updated Spring Boot Workshop",
        "description": "New description for the updated workshop.",
        "eventDate": "2025-09-20T14:00:00",
        "location": "Hybrid - Online & Venue"
    }'
    ```

#### **5. Delete Event**
* **Endpoint:** `DELETE /api/v1/events/{id}`
* **Purpose:** Deletes an event.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` who created the event.
* **Success Response (204 No Content):** No body.
* **Example (Postman/cURL - Admin deleting any event):**
    ```bash
    curl -X DELETE "{{baseUrl}}/api/v1/events/101" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```

---

### Event Registration APIs

#### **1. Register for an Event**
* **Endpoint:** `POST /api/v1/registrations/events/{eventId}`
* **Purpose:** Registers the current authenticated user for a specific event.
* **Authentication:** Authenticated.
* **Success Response (201 Created):** `EventRegistrationResponse` DTO.
    ```json
    {
        "id": 201,
        "userId": 1,
        "username": "testuser",
        "eventId": 101,
        "eventName": "Spring Boot Workshop",
        "registrationDate": "2025-07-13T15:30:00",
        "status": "REGISTERED"
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/registrations/events/101" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

#### **2. Get My Event Registrations**
* **Endpoint:** `GET /api/v1/registrations/me`
* **Purpose:** Retrieves all events the current authenticated user has registered for.
* **Authentication:** Authenticated.
* **Success Response (200 OK):** List of `EventRegistrationResponse` DTOs.
    ```json
    [
        {
            "id": 201,
            "userId": 1,
            "username": "testuser",
            "eventId": 101,
            "eventName": "Spring Boot Workshop",
            "registrationDate": "2025-07-13T15:30:00",
            "status": "REGISTERED"
        }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/registrations/me" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

#### **3. Get All Registrations for an Event**
* **Endpoint:** `GET /api/v1/registrations/events/{eventId}`
* **Purpose:** Retrieves all users registered for a specific event.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` of that event.
* **Success Response (200 OK):** List of `EventRegistrationResponse` DTOs.
    ```json
    [
        {
            "id": 201,
            "userId": 1,
            "username": "testuser",
            "eventId": 101,
            "eventName": "Spring Boot Workshop",
            "registrationDate": "2025-07-13T15:30:00",
            "status": "REGISTERED"
        },
        {
            "id": 202,
            "userId": 3,
            "username": "another_user",
            "eventId": 101,
            "eventName": "Spring Boot Workshop",
            "registrationDate": "2025-07-13T16:00:00",
            "status": "REGISTERED"
        }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/registrations/events/101" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_OR_ADMIN_ACCESS_TOKEN>"
    ```

#### **4. Unregister from an Event**
* **Endpoint:** `DELETE /api/v1/registrations/events/{eventId}`
* **Purpose:** Removes the current authenticated user's registration from an event.
* **Authentication:** Authenticated (user must be registered for the event).
* **Success Response (204 No Content):** No body.
* **Example (Postman/cURL):**
    ```bash
    curl -X DELETE "{{baseUrl}}/api/v1/registrations/events/101" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

---

## 5. Data Models (DTOs)

This section outlines the primary Data Transfer Objects (DTOs) used for API requests and responses.

* **`AuthRequest`**
    ```java
    public record AuthRequest(String username, String password) {}
    ```

* **`AuthResponse`**
    ```java
    public record AuthResponse(String token, String refreshToken) {}
    ```

* **`RefreshTokenRequest`**
    ```java
    public record RefreshTokenRequest(String refreshToken) {}
    ```

* **`UserResponse`**
    ```java
    public record UserResponse(Long id, String username, String email, Set<Role> roles) {}
    ```

* **`UserUpdateRequest`**
    ```java
    public record UserUpdateRequest(String username, String email, Set<Role> roles) {}
    ```

* **`EventRequest`**
    ```java
    public record EventRequest(String name, String description, LocalDateTime eventDate, String location) {}
    ```

* **`EventResponse`**
    ```java
    public record EventResponse(Long id, String name, String description, LocalDateTime eventDate, String location, Long organizerId, String organizerUsername, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    ```

* **`EventRegistrationResponse`**
    ```java
    public record EventRegistrationResponse(Long id, Long userId, String username, Long eventId, String eventName, LocalDateTime registrationDate, String status) {}
    ```

## 6. Error Handling

The API provides consistent error responses through a `GlobalExceptionHandler`:

* **`400 Bad Request`**: For invalid request body (validation errors) or business logic validation failures (`ValidationException`).
* **`401 Unauthorized`**: For missing or invalid authentication tokens.
* **`403 Forbidden`**: For valid tokens but insufficient permissions (`AccessDeniedException`).
* **`404 Not Found`**: For resources not found (`ResourceNotFoundException`).
* **`409 Conflict`**: For resource creation conflicts (e.g., `UserAlreadyExistsException`).
* **`500 Internal Server Error`**: For unexpected server-side errors.
