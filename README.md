
# Booking Management System Application API Documentation
The API documentation for "A booking Management System for "everything""

---

[view in Postman](https://.postman.co/workspace/My-Workspace~00c94159-e798-494b-8b70-a960eabccaab/collection/27269870-2cfef214-1914-4e1f-914e-b25386455771?action=share&creator=27269870&active-environment=27269870-6c68f93c-c24d-4e3b-86fe-d66515ca9c07)

or
## Table of Contents

1.  [Technologies Used](#1-technologies-used)
2.  [Getting Started](#2-getting-started)
  * [Prerequisites](#prerequisites)
  * [Local Setup](#local-setup)
  * [Database Migration](#database-migration)
3.  [Authentication & Authorization](#3-authentication--authorization)
  * [Roles](#roles)
  * [JWT Flow](#jwt-flow)
  * [API Key Configuration](#api-key-configuration)
4.  [API Endpoints](#4-api-endpoints)
  * [Authentication APIs](#authentication-apis)
  * [User APIs](#user-apis)
  * [Bookable Item APIs](#bookable-item-apis)
  * [Booking APIs](#booking-apis)
  * [Availability APIs](#availability-apis)
  * [Recommendation APIs](#recommendation-apis)
  * [Report APIs](#report-apis)
5.  [Advanced Features & Testing](#5-advanced-features--testing)
  * [Private Bookable Items](#private-bookable-items)
  * [Conflict Detection](#conflict-detection)
  * [Dynamic Availability Calculation](#dynamic-availability-calculation)
  * [Recommendation System (Content-Based)](#recommendation-system-content-based)
  * [Dynamic Pricing (Demand-Based Tiers)](#dynamic-pricing-demand-based-tiers)
  * [Notification Orchestration (Outbox Pattern)](#notification-orchestration-outbox-pattern)
  * [API Rate Limiting](#api-rate-limiting)
6.  [Data Models (DTOs)](#6-data-models-dtos)
7.  [Error Handling](#7-error-handling)

---

## 1. Technologies Used

* **Spring Boot:** Framework for building robust, production-ready Spring applications.
* **Spring Security:** Comprehensive security services for Java EE-based enterprise software applications.
* **Spring Data JPA / Hibernate:** For database interaction and ORM.
* **JWT (JSON Web Tokens):** For stateless authentication.
* **BCrypt:** Secure password hashing.
* **Lombok:** Reduces boilerplate code (getters, setters, constructors).
* **MySQL:** Relational database.
* **Maven:** Build automation tool.
* **Java 17+:** Programming language.
* **Jackson Datatype JSR310:** For proper `java.time` (LocalDateTime) JSON serialization/deserialization.
* **Bucket4j:** Java rate limiting library based on the token-bucket algorithm.

---

## 2. Getting Started

### Prerequisites

* Java Development Kit (JDK) 17 or higher
* Maven 3.6+
* A **MySQL** database instance
* Postman or a similar API testing tool

### Local Setup

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd <folder-name>
    ```

2.  **Configure `application.properties` (or `application.yml`):**
    Create `src/main/resources/application.properties` and add your database, JWT, and new feature configurations.

    ```properties
    # Database Configuration (Example for MySQL)
    spring.datasource.url=jdbc:mysql://localhost:3306/welcome_db?createDatabaseIfNotExist=true
    spring.datasource.username=your_db_user
    spring.datasource.password=your_db_password
    spring.jpa.hibernate.ddl-auto=update # Recommended for development. Use 'none' or 'validate' for production.
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true

    # JWT Security Configuration
    application.security.jwt.secret-key=YOUR_SUPER_SECRET_KEY_AT_LEAST_256_BITS_LONG_AND_BASE64_ENCODED_FOR_PRODUCTION # Replace with a strong, base64-encoded key
    application.security.jwt.expiration=900000  # 15 minutes in milliseconds
    application.security.jwt.refresh-token.expiration=604800000 # 7 days in milliseconds

    # Outbox Processor Configuration
    outbox.processor.batch-size=10
    outbox.processor.max-retries=5

    # Rate Limiting Configuration
    app.rate-limit.enabled=true
    app.rate-limit.capacity=100      # Max requests in a burst
    app.rate-limit.refill-rate=10    # Tokens added per second
    app.rate-limit.duration-seconds=1 # The duration over which refill-rate applies
    ```
  * **`secret-key` Note:** For production, generate a strong, random Base64-encoded key. You can generate one using `Base64.getEncoder().encodeToString(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded())` in Java.

3.  **Enable Scheduling and Async:**
    Ensure your main application class (`WelcomeApplication.java`) has the following annotations:
    ```java
    import org.springframework.scheduling.annotation.EnableAsync;
    import org.springframework.scheduling.annotation.EnableScheduling;

    @SpringBootApplication
    @EnableScheduling
    @EnableAsync
    public class WelcomeApplication {
        // ...
    }
    ```

4.  **Build the project:**
    ```bash
    mvn clean install
    ```

5.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080` (or your configured port).

### Database Migration

If `spring.jpa.hibernate.ddl-auto` is set to `update`, Hibernate will attempt to manage schema changes automatically. However, for specific column type changes (like `JSON` for `price_tiers` or `TEXT` for `payload` in `outbox_events`), or if `ddl-auto` is `none`/`validate`, you might need to run manual SQL commands:

* **For `bookable_items` table (add `is_private` and `price_tiers`):**
    ```sql
    ALTER TABLE bookable_items
    ADD COLUMN is_private BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN price_tiers JSON NULL; -- Use JSON type for MySQL 5.7+
    ```
* **For `outbox_events` table (refactor columns):**
    ```sql
    ALTER TABLE outbox_events
    DROP COLUMN IF EXISTS username,
    DROP COLUMN IF EXISTS email,
    ADD COLUMN payload TEXT NOT NULL,
    ADD COLUMN recipient_email VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS processed_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(255) NULL;
    ```
  *(Note: `updated_at` and `retry_count` should already exist from previous steps.)*

---

## 3. Authentication & Authorization

The application uses **JWT (JSON Web Tokens)** for stateless authentication and **Role-Based Access Control (RBAC)** for authorization.

### Roles

* **`USER`**: Standard application user. Can book items, view their own profile, and view public bookable items.
* **`EVENT_ORGANIZER`**: Can create, update, and delete bookable items they own. Can view bookings for their own items.
* **`ADMIN`**: Has full administrative privileges across all resources (users, bookable items, bookings, reports).

### JWT Flow

1.  **Login (`POST /api/v1/auth/login`):** Users provide username/password and receive an `accessToken` and `refreshToken`.
2.  **Access Protected Resources:** The `accessToken` is sent in the `Authorization: Bearer <token>` header for subsequent requests to protected endpoints.
3.  **Token Expiration:** The `accessToken` has a short lifespan.
4.  **Token Refresh (`POST /api/v1/auth/refresh`):** When the `accessToken` expires, the `refreshToken` can be used to obtain a new `accessToken` (and often a new `refreshToken` for rotation).

### API Key Configuration

For Postman, it's recommended to set up an environment variable, e.g., `baseUrl` with value `http://localhost:8080`.

---

## 4. API Endpoints

### Authentication APIs

#### 1. Register a New User
* **Endpoint:** `POST /api/v1/user`
* **Purpose:** Creates a new user account. Triggers an asynchronous welcome email via the Outbox Pattern.
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

#### 2. User Login
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

#### 3. Refresh Access Token
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

#### 1. Get Current Authenticated User's Profile
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

#### 2. Get User by ID
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

#### 3. Get All Users
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

#### 4. Update User Profile
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

#### 5. Delete User
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

### Bookable Item APIs

These APIs manage the creation, retrieval, and modification of various bookable items (e.g., events, appointments, resources).

#### 1. Create a Bookable Item
* **Endpoint:** `POST /api/v1/items`
* **Purpose:** Creates a new bookable item. The logged-in user becomes the organizer. Supports optional `isPrivate` and `priceTiers`.
* **Authentication:** `EVENT_ORGANIZER` or `ADMIN` Role.
* **Request Body:** `BookableItemRequest` DTO.
    ```json
    {
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "startTime": "2025-09-15T10:00:00",
        "endTime": "2025-09-15T12:00:00",
        "location": "Online via Zoom",
        "capacity": 50,
        "isPrivate": false,
        "priceTiers": [
            {"tier": "Early Bird", "price": 49.99, "maxCapacity": 20},
            {"tier": "Regular", "price": 59.99, "maxCapacity": 30}
        ]
    }
    ```
* **Success Response (201 Created):** `BookableItemResponse` DTO.
    ```json
    {
        "id": 101,
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "startTime": "2025-09-15T10:00:00",
        "endTime": "2025-09-15T12:00:00",
        "location": "Online via Zoom",
        "capacity": 50,
        "organizerId": 2,
        "organizerUsername": "event_organizer",
        "isPrivate": false,
        "priceTiers": [
            {"tier": "Early Bird", "price": 49.99, "maxCapacity": 20},
            {"tier": "Regular", "price": 59.99, "maxCapacity": 30}
        ],
        "currentPrice": 49.99,
        "availableCapacity": 50,
        "createdAt": "2025-07-13T12:00:00",
        "updatedAt": "2025-07-13T12:00:00"
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/items" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_ACCESS_TOKEN>" \
    -d '{
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "startTime": "2025-09-15T10:00:00",
        "endTime": "2025-09-15T12:00:00",
        "location": "Online via Zoom",
        "capacity": 50,
        "isPrivate": false,
        "priceTiers": [{"tier": "Standard", "price": 59.99, "maxCapacity": 50}]
    }'
    ```

#### 2. Get Bookable Item by ID
* **Endpoint:** `GET /api/v1/items/{id}`
* **Purpose:** Retrieves details of a specific bookable item. Private items are only visible to the organizer or admin.
* **Authentication:** Public for public items. Authenticated (`EVENT_ORGANIZER` for their own private items, `ADMIN` for any private item).
* **Success Response (200 OK):** `BookableItemResponse` DTO.
    ```json
    {
        "id": 101,
        "name": "Spring Boot Workshop",
        "description": "An interactive workshop on Spring Boot basics.",
        "startTime": "2025-09-15T10:00:00",
        "endTime": "2025-09-15T12:00:00",
        "location": "Online via Zoom",
        "capacity": 50,
        "organizerId": 2,
        "organizerUsername": "event_organizer",
        "isPrivate": false,
        "priceTiers": [
            {"tier": "Early Bird", "price": 49.99, "maxCapacity": 20},
            {"tier": "Regular", "price": 59.99, "maxCapacity": 30}
        ],
        "currentPrice": 49.99,
        "availableCapacity": 50,
        "createdAt": "2025-07-13T12:00:00",
        "updatedAt": "2025-07-13T12:00:00"
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/items/101"
    ```

#### 3. Get All Bookable Items
* **Endpoint:** `GET /api/v1/items`
* **Purpose:** Retrieves a list of all available public bookable items. Includes filtering by `organizerId`.
* **Authentication:** Public.
* **Query Parameters:**
  * `organizerId` (Optional): Filter items by organizer ID.
  * `onlyMyItems` (Optional): If true, returns only items organized by the authenticated user. Requires authentication.
* **Success Response (200 OK):** List of `BookableItemResponse` DTOs.
    ```json
    [
        {
            "id": 101,
            "name": "Spring Boot Workshop",
            "description": "An interactive workshop on Spring Boot basics.",
            "startTime": "2025-09-15T10:00:00",
            "endTime": "2025-09-15T12:00:00",
            "location": "Online via Zoom",
            "capacity": 50,
            "organizerId": 2,
            "organizerUsername": "event_organizer",
            "isPrivate": false,
            "priceTiers": [
                {"tier": "Early Bird", "price": 49.99, "maxCapacity": 20},
                {"tier": "Regular", "price": 59.99, "maxCapacity": 30}
            ],
            "currentPrice": 49.99,
            "availableCapacity": 50,
            "createdAt": "2025-07-13T12:00:00",
            "updatedAt": "2025-07-13T12:00:00"
        }
    ]
    ```
* **Example (Postman/cURL - Get all public items):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/items"
    ```
* **Example (Postman/cURL - Get items by a specific organizer):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/items?organizerId=2"
    ```
* **Example (Postman/cURL - Get items organized by current user):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/items?onlyMyItems=true" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_ACCESS_TOKEN>"
    ```

#### 4. Update Bookable Item
* **Endpoint:** `PUT /api/v1/items/{id}`
* **Purpose:** Updates an existing bookable item's details. Can also modify `isPrivate` and `priceTiers`.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` who created the item.
* **Request Body:** `BookableItemRequest` DTO.
    ```json
    {
        "name": "Updated Spring Boot Workshop",
        "description": "New description for the updated workshop.",
        "startTime": "2025-09-20T14:00:00",
        "endTime": "2025-09-20T16:00:00",
        "location": "Hybrid - Online & Venue",
        "capacity": 60,
        "isPrivate": true,
        "priceTiers": [
            {"tier": "Standard", "price": 65.00, "maxCapacity": 60}
        ]
    }
    ```
* **Success Response (200 OK):** Updated `BookableItemResponse` DTO.
* **Example (Postman/cURL - Organizer updating their item):**
    ```bash
    curl -X PUT "{{baseUrl}}/api/v1/items/101" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_ACCESS_TOKEN>" \
    -d '{
        "name": "Updated Spring Boot Workshop",
        "description": "New description for the updated workshop.",
        "startTime": "2025-09-20T14:00:00",
        "endTime": "2025-09-20T16:00:00",
        "location": "Hybrid - Online & Venue",
        "capacity": 60,
        "isPrivate": true,
        "priceTiers": [{"tier": "Standard", "price": 65.00, "maxCapacity": 60}]
    }'
    ```

#### 5. Delete Bookable Item
* **Endpoint:** `DELETE /api/v1/items/{id}`
* **Purpose:** Deletes a bookable item.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` who created the item.
* **Success Response (204 No Content):** No body.
* **Example (Postman/cURL - Admin deleting any item):**
    ```bash
    curl -X DELETE "{{baseUrl}}/api/v1/items/101" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```

---

### Booking APIs

These APIs handle the actual booking process and management of user bookings.

#### 1. Create a Booking for an Item
* **Endpoint:** `POST /api/v1/bookings/items/{itemId}`
* **Purpose:** Creates a new booking for the current authenticated user for a specific bookable item. Checks for availability and conflicts.
* **Authentication:** Authenticated (`USER`, `EVENT_ORGANIZER`, `ADMIN`).
* **Request Body:** `BookingRequest` DTO (optional if using default tier, required if specifying a price tier).
    ```json
    {
        "priceTier": "Early Bird"
    }
    ```
* **Success Response (201 Created):** `BookingResponse` DTO. Triggers asynchronous booking confirmation email.
    ```json
    {
        "id": 201,
        "userId": 1,
        "username": "testuser",
        "itemId": 101,
        "itemName": "Spring Boot Workshop",
        "bookingDate": "2025-07-13T15:30:00",
        "status": "CONFIRMED",
        "pricePaid": 49.99
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X POST "{{baseUrl}}/api/v1/bookings/items/101" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>" \
    -d '{
        "priceTier": "Early Bird"
    }'
    ```

#### 2. Get My Bookings
* **Endpoint:** `GET /api/v1/bookings/me`
* **Purpose:** Retrieves all upcoming and past bookings for the current authenticated user.
* **Authentication:** Authenticated.
* **Success Response (200 OK):** List of `BookingResponse` DTOs.
    ```json
    [
        {
            "id": 201,
            "userId": 1,
            "username": "testuser",
            "itemId": 101,
            "itemName": "Spring Boot Workshop",
            "bookingDate": "2025-07-13T15:30:00",
            "status": "CONFIRMED",
            "pricePaid": 49.99
        }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/bookings/me" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

#### 3. Get All Bookings for a Specific Item
* **Endpoint:** `GET /api/v1/bookings/items/{itemId}`
* **Purpose:** Retrieves all bookings for a specific bookable item.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` of that item.
* **Success Response (200 OK):** List of `BookingResponse` DTOs.
    ```json
    [
        {
            "id": 201,
            "userId": 1,
            "username": "testuser",
            "itemId": 101,
            "itemName": "Spring Boot Workshop",
            "bookingDate": "2025-07-13T15:30:00",
            "status": "CONFIRMED",
            "pricePaid": 49.99
        },
        {
            "id": 202,
            "userId": 3,
            "username": "another_user",
            "itemId": 101,
            "itemName": "Spring Boot Workshop",
            "bookingDate": "2025-07-13T16:00:00",
            "status": "CONFIRMED",
            "pricePaid": 59.99
        }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/bookings/items/101" \
    -H "Authorization: Bearer <EVENT_ORGANIZER_OR_ADMIN_ACCESS_TOKEN>"
    ```

#### 4. Cancel a Booking
* **Endpoint:** `PUT /api/v1/bookings/{bookingId}/cancel`
* **Purpose:** Cancels an existing booking. Can be done by the user who made the booking or an admin. Triggers asynchronous cancellation email.
* **Authentication:** Authenticated (user must own the booking or have `ADMIN` role).
* **Success Response (200 OK):** Updated `BookingResponse` DTO with `CANCELLED` status.
* **Example (Postman/cURL):**
    ```bash
    curl -X PUT "{{baseUrl}}/api/v1/bookings/201/cancel" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

#### 5. Update Booking Status (Admin/Organizer)
* **Endpoint:** `PUT /api/v1/bookings/{bookingId}/status`
* **Purpose:** Allows an admin or organizer to manually update a booking's status.
* **Authentication:** `ADMIN` Role OR the `EVENT_ORGANIZER` of the associated item.
* **Request Body:** `BookingStatusUpdateRequest` DTO.
    ```json
    {
        "status": "COMPLETED"
    }
    ```
* **Success Response (200 OK):** Updated `BookingResponse` DTO.
* **Example (Postman/cURL):**
    ```bash
    curl -X PUT "{{baseUrl}}/api/v1/bookings/201/status" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
    -d '{
        "status": "COMPLETED"
    }'
    ```

---

### Availability APIs

These APIs provide insights into the real-time availability of bookable items.

#### 1. Get Available Capacity for a Bookable Item
* **Endpoint:** `GET /api/v1/items/{itemId}/availability`
* **Purpose:** Returns the current available capacity for a given bookable item, considering its total capacity and existing confirmed bookings.
* **Authentication:** Public.
* **Success Response (200 OK):** `AvailableCapacityResponse` DTO.
    ```json
    {
        "itemId": 101,
        "itemName": "Spring Boot Workshop",
        "totalCapacity": 50,
        "bookedSlots": 10,
        "availableSlots": 40
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/items/101/availability"
    ```

#### 2. Check for Time Conflicts for a User
* **Endpoint:** `GET /api/v1/users/me/conflicts?startTime={startTime}&endTime={endTime}`
* **Purpose:** Checks if the authenticated user has any existing bookings that conflict with a specified time range.
* **Authentication:** Authenticated.
* **Query Parameters:**
  * `startTime`: Start time of the period to check (e.g., `2025-09-15T09:00:00`).
  * `endTime`: End time of the period to check (e.g., `2025-09-15T11:00:00`).
* **Success Response (200 OK):** `UserConflictCheckResponse` DTO.
    ```json
    {
        "userId": 1,
        "hasConflict": true,
        "conflictingBookings": [
            {
                "id": 201,
                "itemName": "Another Meeting",
                "startTime": "2025-09-15T09:30:00",
                "endTime": "2025-09-15T10:30:00"
            }
        ]
    }
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/users/me/conflicts?startTime=2025-09-15T09:00:00&endTime=2025-09-15T11:00:00" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

---

### Recommendation APIs

This API provides recommendations for bookable items based on a content-based filtering approach.

#### 1. Get Recommended Items for User
* **Endpoint:** `GET /api/v1/recommendations/users/me`
* **Purpose:** Returns a list of bookable items recommended for the authenticated user based on their past booking history and the characteristics of those items (e.g., location, type, organizer).
* **Authentication:** Authenticated.
* **Query Parameters:**
  * `limit` (Optional): Maximum number of recommendations to return (default: 5).
* **Success Response (200 OK):** List of `BookableItemResponse` DTOs.
    ```json
    [
        {
            "id": 105,
            "name": "Advanced Spring Security Workshop",
            "description": "Deep dive into Spring Security.",
            "startTime": "2025-10-01T09:00:00",
            "endTime": "2025-10-01T17:00:00",
            "location": "Online via Zoom",
            "capacity": 30,
            "organizerId": 2,
            "organizerUsername": "event_organizer",
            "isPrivate": false,
            "priceTiers": [
                {"tier": "Standard", "price": 99.99, "maxCapacity": 30}
            ],
            "currentPrice": 99.99,
            "availableCapacity": 25,
            "createdAt": "2025-07-10T10:00:00",
            "updatedAt": "2025-07-10T10:00:00"
        },
        {
            "id": 106,
            "name": "Jakarta EE Basics",
            "description": "Introduction to Jakarta EE.",
            "startTime": "2025-09-25T13:00:00",
            "endTime": "2025-09-25T16:00:00",
            "location": "Conference Room A",
            "capacity": 20,
            "organizerId": 4,
            "organizerUsername": "another_organizer",
            "isPrivate": false,
            "priceTiers": [
                {"tier": "Standard", "price": 75.00, "maxCapacity": 20}
            ],
            "currentPrice": 75.00,
            "availableCapacity": 18,
            "createdAt": "2025-07-11T11:00:00",
            "updatedAt": "2025-07-11T11:00:00"
        }
    ]
    ```
* **Example (Postman/cURL):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/recommendations/users/me?limit=3" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
    ```

---

### Report APIs

These APIs provide administrative reporting capabilities.

#### 1. Get Booking Revenue Report
* **Endpoint:** `GET /api/v1/reports/revenue`
* **Purpose:** Generates a report on total revenue, optionally filtered by `itemId` and/or a date range. Only `ADMIN` can access this.
* **Authentication:** `ADMIN` Role.
* **Query Parameters:**
  * `itemId` (Optional): Filter revenue for a specific bookable item.
  * `startDate` (Optional): Start date for the report (e.g., `2025-01-01`).
  * `endDate` (Optional): End date for the report (e.g., `2025-12-31`).
* **Success Response (200 OK):** `RevenueReportResponse` DTO.
    ```json
    {
        "totalRevenue": 1500.75,
        "reportDetails": [
            {
                "itemId": 101,
                "itemName": "Spring Boot Workshop",
                "itemRevenue": 750.50,
                "numberOfBookings": 15
            },
            {
                "itemId": 102,
                "itemName": "Team Building Session",
                "itemRevenue": 750.25,
                "numberOfBookings": 10
            }
        ]
    }
    ```
* **Example (Postman/cURL - Total Revenue):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/reports/revenue" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```
* **Example (Postman/cURL - Revenue for a specific item in a date range):**
    ```bash
    curl -X GET "{{baseUrl}}/api/v1/reports/revenue?itemId=101&startDate=2025-07-01&endDate=2025-09-30" \
    -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
    ```

---

## 5. Advanced Features & Testing

This section details advanced features implemented and provides guidance on how to test them.

### Private Bookable Items

* **Feature:** Bookable items can be marked as `isPrivate=true`. Private items are only discoverable and viewable by their `organizerId` or an `ADMIN`. They will not appear in the general `/api/v1/items` listing for regular users.
* **Testing:**
  1.  Create a user with `EVENT_ORGANIZER` role (`organizer1`).
  2.  `organizer1` creates a private item.
  3.  Attempt to `GET /api/v1/items/{privateItemId}` as `organizer1` (should succeed).
  4.  Attempt to `GET /api/v1/items/{privateItemId}` as a `USER` (should return 404 Not Found or 403 Forbidden).
  5.  Attempt to `GET /api/v1/items` as a `USER` (private item should not be in the list).
  6.  Attempt to `GET /api/v1/items/{privateItemId}` as `ADMIN` (should succeed).
  7.  Have a `USER` try to book a private item (should return an error indicating the item isn't found or accessible).

### Conflict Detection

* **Feature:** When a user attempts to create a booking, the system checks if the new booking's time (`startTime` to `endTime` of the `BookableItem`) overlaps with any of their existing **confirmed** bookings. If a conflict is detected, the booking is rejected.
* **Testing:**
  1.  Create a `USER`.
  2.  Create two public `BookableItem`s (`itemA`, `itemB`) with overlapping `startTime` and `endTime`.
  3.  Have the `USER` successfully book `itemA`.
  4.  Have the `USER` attempt to book `itemB`. This request should result in a `409 Conflict` error with a message indicating the time conflict.
  5.  Verify that booking an item that *doesn't* conflict with existing bookings succeeds.

### Dynamic Availability Calculation

* **Feature:** The `availableCapacity` and `currentPrice` for a `BookableItem` are calculated dynamically at the time of retrieval (`GET /api/v1/items/{id}` and `GET /api/v1/items`). `availableCapacity` is `totalCapacity - confirmedBookingsCount`. `currentPrice` is determined by the `priceTiers` based on how many slots are remaining.
* **Testing:**
  1.  Create a `BookableItem` with `capacity` = 50 and two `priceTiers`: "Early Bird" (price 49.99, maxCapacity 20) and "Regular" (price 59.99, maxCapacity 30).
  2.  `GET /api/v1/items/{itemId}`: Verify `availableCapacity` is 50 and `currentPrice` is 49.99.
  3.  Have 10 `USER`s book this item (using "Early Bird" tier implicitly or explicitly).
  4.  `GET /api/v1/items/{itemId}`: Verify `availableCapacity` is 40 and `currentPrice` is still 49.99.
  5.  Have 15 more `USER`s book this item (total 25 confirmed bookings).
  6.  `GET /api/v1/items/{itemId}`: Verify `availableCapacity` is 25 and `currentPrice` is 59.99 (as "Early Bird" tier is now full, price moves to next tier).
  7.  Attempt to book the item once `availableCapacity` reaches 0 (should result in `400 Bad Request` or similar indicating no more slots).

### Recommendation System (Content-Based)

* **Feature:** The `/api/v1/recommendations/users/me` endpoint provides personalized recommendations. It identifies categories or organizers from the user's past confirmed bookings and suggests other items within those categories/by those organizers that the user hasn't booked yet.
* **Testing:**
  1.  Create multiple `BookableItem`s with different `organizerId`s and categories (e.g., "Tech Workshop" by `organizer1`, "Fitness Class" by `organizer2`, "Another Tech Workshop" by `organizer1`).
  2.  Have a `USER` book "Tech Workshop" by `organizer1`.
  3.  `GET /api/v1/recommendations/users/me` as that `USER`. The response should prioritize "Another Tech Workshop" by `organizer1` and other tech-related items if they exist. Items by `organizer2` or in other categories should be less likely unless a user has booked from diverse categories.
  4.  Book more diverse items and observe how recommendations change.

### Dynamic Pricing (Demand-Based Tiers)

* **Feature:** Bookable items can have `priceTiers`, an array of objects specifying `tier` name, `price`, and `maxCapacity` for that tier. The `currentPrice` of the item (and the price a user pays) is determined by the highest `price` tier for which `availableCapacity` still exists within its `maxCapacity`.
* **Testing:** (Covered partly in Dynamic Availability Calculation)
  1.  Create an item with tiers:
      ```json
      "priceTiers": [
          {"tier": "Super Early Bird", "price": 29.99, "maxCapacity": 5},
          {"tier": "Early Bird", "price": 49.99, "maxCapacity": 20},
          {"tier": "Regular", "price": 69.99, "maxCapacity": 25}
      ]
      ```
      Total capacity = 50.
  2.  **Initial:** `currentPrice` should be 29.99.
  3.  Book 5 slots: `currentPrice` should become 49.99.
  4.  Book another 20 slots (total 25): `currentPrice` should become 69.99.
  5.  Attempt to book more than the item's total capacity (e.g., 51st booking) — should fail with an appropriate error (e.g., "Capacity exceeded").
  6.  Verify that when a user books, the `pricePaid` in the `BookingResponse` reflects the `currentPrice` at the moment of booking.

### Notification Orchestration (Outbox Pattern)

* **Feature:** Instead of directly sending emails, the application uses the Outbox Pattern. When a user registers or a booking is made/cancelled, a corresponding `OutboxEvent` is saved to the database within the same transaction. A scheduled job (`OutboxProcessor`) then asynchronously picks up these events and simulates sending notifications (e.g., logging them) without blocking the main request thread. It includes retry logic.
* **Testing:**
  1.  Set `spring.jpa.hibernate.ddl-auto=update` or manually create the `outbox_events` table as specified in Database Migration.
  2.  Register a new user via `POST /api/v1/user`.
  3.  Immediately after the successful response, query the `outbox_events` table in your database. You should see a new entry with `eventType='USER_REGISTERED'` and `processed_at` as NULL.
  4.  Wait a few seconds (the scheduler interval).
  5.  Query the `outbox_events` table again. The `processed_at` column for that event should now have a timestamp, indicating it was processed, and you should see a log message in the application console simulating the email sending.
  6.  Repeat for a successful `POST /api/v1/bookings/items/{itemId}` (event type `BOOKING_CONFIRMED`) and a `PUT /api/v1/bookings/{bookingId}/cancel` (event type `BOOKING_CANCELLED`).
  7.  To test retry logic: Temporarily configure the email service (or mock it) to fail, then observe `retry_count` and `error_message` updating in `outbox_events`.

### API Rate Limiting

* **Feature:** Protects certain endpoints (e.g., login, registration) from excessive requests using the Bucket4j library to prevent brute-force attacks or denial-of-service. Configurable via `application.properties`.
* **Testing:**
  1.  Ensure `app.rate-limit.enabled=true` and set `app.rate-limit.capacity` to a low number (e.g., 5) and `app.rate-limit.refill-rate` to 1 token per second.
  2.  Rapidly send more than `capacity` (e.g., 6 or more) `POST /api/v1/auth/login` requests from the same IP address/client.
  3.  The first few requests should succeed. Subsequent requests exceeding the limit within the defined period should receive a `429 Too Many Requests` HTTP status code.
  4.  Wait for the `refill-rate` duration, then try again. Requests should now succeed until the limit is hit again.

---

## 6. Data Models (DTOs)

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

* **`PriceTierDTO`**
    ```java
    public record PriceTierDTO(String tier, double price, int maxCapacity) {}
    ```

* **`BookableItemRequest`**
    ```java
    public record BookableItemRequest(
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        int capacity,
        boolean isPrivate,
        List<PriceTierDTO> priceTiers
    ) {}
    ```

* **`BookableItemResponse`**
    ```java
    public record BookableItemResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        int capacity,
        Long organizerId,
        String organizerUsername,
        boolean isPrivate,
        List<PriceTierDTO> priceTiers,
        double currentPrice,       // Dynamically calculated
        int availableCapacity,     // Dynamically calculated
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
    ```

* **`BookingRequest`**
    ```java
    public record BookingRequest(String priceTier) {} // Optional: specify preferred price tier
    ```

* **`BookingResponse`**
    ```java
    public record BookingResponse(
        Long id,
        Long userId,
        String username,
        Long itemId,
        String itemName,
        LocalDateTime bookingDate,
        BookingStatus status,      // e.g., PENDING, CONFIRMED, CANCELLED, COMPLETED
        double pricePaid
    ) {}
    ```

* **`BookingStatusUpdateRequest`**
    ```java
    public record BookingStatusUpdateRequest(BookingStatus status) {}
    ```

* **`AvailableCapacityResponse`**
    ```java
    public record AvailableCapacityResponse(
        Long itemId,
        String itemName,
        int totalCapacity,
        int bookedSlots,
        int availableSlots
    ) {}
    ```

* **`UserConflictCheckResponse`**
    ```java
    public record UserConflictCheckResponse(
        Long userId,
        boolean hasConflict,
        List<ConflictingBookingDTO> conflictingBookings
    ) {}
    ```
  * **`ConflictingBookingDTO`** (nested within `UserConflictCheckResponse`)
      ```java
      public record ConflictingBookingDTO(Long id, String itemName, LocalDateTime startTime, LocalDateTime endTime) {}
      ```

* **`RevenueReportResponse`**
    ```java
    public record RevenueReportResponse(
        double totalRevenue,
        List<ItemRevenueDetail> reportDetails
    ) {}
    ```
  * **`ItemRevenueDetail`** (nested within `RevenueReportResponse`)
      ```java
      public record ItemRevenueDetail(Long itemId, String itemName, double itemRevenue, long numberOfBookings) {}
      ```

---

## 7. Error Handling

The API provides consistent error responses through a `GlobalExceptionHandler`:

* **`400 Bad Request`**: For invalid request body (validation errors) or business logic validation failures (e.g., `ValidationException`, `CapacityExceededException`).
* **`401 Unauthorized`**: For missing or invalid authentication tokens.
* **`403 Forbidden`**: For valid tokens but insufficient permissions (`AccessDeniedException`).
* **`404 Not Found`**: For resources not found (`ResourceNotFoundException`).
* **`409 Conflict`**: For resource creation conflicts (e.g., `UserAlreadyExistsException`, `TimeConflictException`).
* **`429 Too Many Requests`**: When API rate limits are exceeded.
* **`500 Internal Server Error`**: For unexpected server-side errors.
