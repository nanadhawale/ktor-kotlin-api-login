# Kotlin Ktor API with Login

## Features

- Basic Login API (`/login`)
- Connects to Railway PostgreSQL
- Uses Exposed ORM

## How to Run

1. Update `.env` with your real DATABASE_URL
2. Run: `./gradlew run`

## Endpoint

- `POST /login`  
  JSON body:
  ```json
  {
    "email": "test@example.com",
    "password": "123456"
  }
  ```