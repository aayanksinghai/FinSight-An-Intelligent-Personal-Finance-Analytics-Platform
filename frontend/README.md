# FinSight Frontend (User-Service UI)

Responsive React frontend for the currently implemented `user-service` capabilities.

## Features covered

- User auth: register, login, logout
- Password reset: request token, confirm reset
- Session handling: access + refresh token flow
- Profile: read, update, security overview, change password, delete account
- Admin-lite: list users by status and deactivate/reactivate

## Tech stack

- React 18
- Vite
- React Router
- Axios
- Zustand

## Project structure

```text
frontend/
  src/
    api/            # API modules (auth/profile/admin)
    components/     # Shared UI blocks and route guard
    pages/          # Route screens
    store/          # Zustand auth store + token persistence
    utils/          # JWT helpers
```

## Environment

Copy `.env.example` to `.env` and adjust if needed:

```bash
cp .env.example .env
```

`VITE_API_BASE_URL` defaults to `http://localhost:8090`.

## Run locally

```bash
cd frontend
npm install
npm run dev
```

App opens on Vite default URL (typically `http://localhost:5173`).

## Build

```bash
cd frontend
npm run build
npm run preview
```

## Backend prerequisites

- `user-service` must be running
- Use dev bootstrap accounts:
  - `demo@finsight.local` / `Passw0rd!123`
  - `admin@finsight.local` / `Adm1nP@ss!`

## Quick smoke script

The helper checks `ping`, `login`, and authenticated `/api/users/me`:

```bash
cd frontend
npm run smoke
```

To target another backend URL:

```bash
cd frontend
VITE_API_BASE_URL=http://localhost:8090 npm run smoke
```

