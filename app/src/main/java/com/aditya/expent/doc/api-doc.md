# Expense Tracker - API Reference for Android App

This document provides a comprehensive guide to all the API endpoints available in the backend, along with their expected request payloads, responses, and how they should be utilized in the Android App (e.g., via Retrofit).

## Authentication Flow (Crucial for Android)
The backend uses Google Authentication and JWT Tokens.

### 1. Google Login
- **Endpoint**: `POST /auth/google`
- **Use Case**: Called when the user signs in with Google on the Android app using Credential Manager or Google Sign-In.
- **Request Payload**:
  ```json
  {
    "idToken": "string" // The ID token received from Google Sign-In on Android
  }
  ```
- **Response**:
  ```json
  {
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "name": "User Name"
    },
    "accessToken": "ey...",
    "refreshToken": "ey..."
  }
  ```
  *Note: The Android app MUST save both the `accessToken` and `refreshToken` (e.g., using EncryptedSharedPreferences). The `accessToken` should be attached as a Bearer token in the `Authorization` header for all subsequent API requests.*

### 2. Refresh Token
- **Endpoint**: `POST /auth/refresh`
- **Use Case**: Called when the `accessToken` expires (typically returns a 401 Unauthorized error).
- **Request Headers**: Must include `Cookie: refreshToken=<your_refresh_token>` or adapt backend to accept it via body if cookies are hard to handle in Android Retrofit. Currently, the backend expects the refresh token in the cookie.
- **Response**:
  ```json
  {
    "message": "Tokens refreshed",
    "accessToken": "new_access_token",
    "refreshToken": "new_refresh_token"
  }
  ```

### 3. Get Current User Profile
- **Endpoint**: `GET /auth/me`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Response**: Returns the `User` object.

### 4. Update Profile
- **Endpoint**: `PUT /auth/profile`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Request Payload**:
  ```json
  {
    "name": "string (optional)",
    "timezone": "string (optional)",
    "currency": "string (optional)"
  }
  ```
- **Response**: Returns the updated `User` object.

---

## Transactions
Manage income, expenses, and transfers.

### 1. Get All Transactions
- **Endpoint**: `GET /transactions`
- **Query Parameters**:
    - `page`: number (default 1)
    - `limit`: number (default 10)
    - `startDate`, `endDate`: string (ISO dates)
    - `type`: "INCOME" | "EXPENSE" | "TRANSFER"
    - `accountId`, `categoryId`: string
- **Response**: Paginated list of `Transaction` objects.

### 2. Create Transaction
- **Endpoint**: `POST /transactions`
- **Request Payload**:
  ```json
  {
    "accountId": "uuid",
    "categoryId": "uuid (optional for transfers)",
    "transferToAccountId": "uuid (optional)",
    "type": "INCOME | EXPENSE | TRANSFER",
    "amount": "number",
    "transactionDate": "ISO-8601 Date String",
    "note": "string (optional)",
    "merchant": "string (optional)",
    "paymentMethod": "string (optional)",
    "isSalary": "boolean (optional)"
  }
  ```
- **Response**: The created `Transaction` object.

### 3. Update / Delete Transaction
- **Endpoints**: `PUT /transactions/:id`, `DELETE /transactions/:id`
- **PUT Payload**: Same fields as Create (all optional).
- **Response**: Updated `Transaction` / Success Message.

---

## Accounts
Manage user accounts (Bank, Wallet, etc.)

### 1. Get Accounts
- **Endpoint**: `GET /accounts`
- **Response**: Array of `Account` objects.

### 2. Create Account
- **Endpoint**: `POST /accounts`
- **Request Payload**:
  ```json
  {
    "name": "string",
    "openingBalance": "number",
    "type": "PAY_NOW | PAY_LATER" // Optional
  }
  ```
- **Response**: The created `Account` object.

### 3. Update / Delete Account
- **Endpoints**: `PUT /accounts/:id`, `DELETE /accounts/:id`
- **PUT Payload**: Partial Account data (name, isActive, etc.).

---

## Categories
Manage income/expense categories.

### 1. Get Categories
- **Endpoint**: `GET /categories`
- **Response**: Array of `Category` objects.

### 2. Create Category
- **Endpoint**: `POST /categories`
- **Request Payload**:
  ```json
  {
    "name": "string",
    "type": "INCOME | EXPENSE",
    "parentId": "uuid (optional)",
    "color": "string (optional hex code)",
    "icon": "string (optional icon name)"
  }
  ```

---

## Budgets
Manage spending limits.

### 1. Get Budgets & Budget Status
- **Endpoints**: `GET /budgets`, `GET /budgets/status`
- **Response**: Array of `Budget` objects. `/status` returns budgets along with the current spent amount.

### 2. Create Budget
- **Endpoint**: `POST /budgets`
- **Request Payload**:
  ```json
  {
    "categoryId": "uuid (optional, null means overall budget)",
    "periodType": "WEEKLY | MONTHLY | SALARY_CYCLE | CUSTOM",
    "limitAmount": "number",
    "startDate": "ISO String",
    "endDate": "ISO String (optional)"
  }
  ```

---

## Credit Cards & EMIs
Manage credit lines and loans.

### 1. Credit Cards CRUD
- **Endpoints**: `GET /credit-cards`, `POST /credit-cards`, `PUT /credit-cards/:id`
- **POST Payload**:
  ```json
  {
    "accountId": "uuid",
    "cardName": "string",
    "limit": "number",
    "statementDate": "number (1-31)",
    "dueDays": "number (days after statement)"
  }
  ```

### 2. EMIs CRUD
- **Endpoints**: `GET /emis`, `POST /emis`, `PUT /emis/:id`
- **POST Payload**:
  ```json
  {
    "accountId": "uuid",
    "name": "string",
    "principal": "number",
    "tenure": "number (months)",
    "monthlyEmi": "number",
    "nextDueDate": "ISO String"
  }
  ```

---

## Analytics & Dashboard
Used for generating charts and overview data on the home screen.

### 1. Dashboard Overview
- **Endpoint**: `GET /dashboard`
- **Response**: Aggregated data (Total Balance, Income, Expenses, Recent Transactions).

### 2. Analytics
- **Endpoints**:
    - `GET /analytics/summary`
    - `GET /analytics/daily`
    - `GET /analytics/weekly`
    - `GET /analytics/monthly`
    - `GET /analytics/category-wise`
    - `GET /analytics/cashflow`
- **Response**: Aggregated JSON data suitable for rendering charts (e.g., MPAndroidChart or Compose Canvas).

---

## Android Integration Guide (Retrofit)

1. **Token Interceptor**: Create an OkHttp `Interceptor` that attaches the `Authorization: Bearer <accessToken>` header to every request except `/auth/google`.
2. **Authenticator**: Implement an OkHttp `Authenticator` that triggers a call to `/auth/refresh` if an API returns a 401 Unauthorized status. Note that the backend expects the refresh token in a cookie. You may need to manually read the `Set-Cookie` header during login and send it back via a `Cookie` header during refresh, OR update the backend to also accept it via the JSON body for easier mobile consumption.
3. **Data Classes**: Map the above JSON payloads to Kotlin `data class` with `@SerializedName` (Gson) or `@Json` (Moshi/Kotlinx Serialization).
4. **Dates**: Use `java.time.OffsetDateTime` or `Instant` and configure your JSON parser to handle ISO-8601 strings, as the backend uses Prisma standard Date/Timestamptz formats.
