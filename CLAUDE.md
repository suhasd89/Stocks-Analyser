# Stock Signal Tracker - Project Guide

## Project Overview

Indian Stock Signal Tracker is a Spring Boot backend + React frontend application for analyzing stock signals and patterns. It provides real-time stock scanning using multiple strategies (SMA, V20, Multibagger) with email notification support.

## Architecture

### Backend (Java 25 + Spring Boot 4.0.x)
- **Location**: `./backend`
- **Framework**: Spring Boot 4.0.6 with Maven
- **Database**: SQLite
- **Key Features**:
  - Multiple scanning strategies (SMA, V20, Multibagger)
  - Virtual threads for concurrent stock scanning
  - SMTP-based email notifications
  - REST API endpoints for dashboard and admin operations
  - Security layer with user authentication

### Frontend (React 18 + Vite)
- **Location**: `./frontend`
- **Framework**: React 18 with Vite 5
- **Features**:
  - Dashboard with multiple strategy views
  - Dark/light theme toggle
  - Watchlist manager
  - Real-time scanner alerts
  - Responsive UI with email/WhatsApp sharing

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Data Persistence**: SQLite database with volume mount
- **Reverse Proxy**: Nginx (frontend container)

## Setup & Development

### Prerequisites
- **Backend**: JDK 25, Maven 3.8+
- **Frontend**: Node.js 18+, npm 9+
- **Docker**: Docker Engine 20.10+, Docker Compose v2+

### Local Development

#### Backend
```bash
cd backend
mvn spring-boot:run
```
Runs on `http://localhost:8080`

#### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`

### Docker Development
```bash
cp .env.example .env
docker compose up --build
```
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080/api/health`

## Code Style & Conventions

### Java Backend
- **Language Level**: Java 25 (Project Loom virtual threads)
- **Formatting**: 4-space indentation
- **Naming**: camelCase for variables/methods, PascalCase for classes
- **Packages**: `com.suhas.stocktracker.*`

### Frontend (JavaScript/React)
- **Indentation**: 2-space
- **Formatting**: Prettier (configured in `.prettierrc.json`)
- **Linting**: ESLint with React plugin (configured in `.eslintrc.cjs`)
- **Naming**: camelCase for variables/functions, PascalCase for components

### EditorConfig
All editors should respect `.editorconfig` for consistent formatting.

## Key Files & Directories

```
.
├── backend/
│   ├── src/main/java/com/suhas/stocktracker/
│   │   ├── controller/          # REST endpoints
│   │   ├── service/             # Business logic (scanning, notifications)
│   │   ├── model/               # Data models
│   │   ├── config/              # Spring configuration
│   │   └── repository/          # Data access layer
│   ├── src/main/resources/
│   │   ├── application.yml      # Spring Boot configuration
│   │   └── watchlists/          # YAML watchlist definitions
│   └── pom.xml                  # Maven configuration
├── frontend/
│   ├── src/
│   │   ├── App.jsx              # Main React component
│   │   ├── main.jsx             # Entry point
│   │   └── styles.css           # Global styles
│   ├── package.json             # npm dependencies
│   ├── vite.config.js           # Vite build configuration
│   ├── .eslintrc.cjs            # ESLint configuration
│   └── .prettierrc.json         # Prettier configuration
├── .editorconfig                # Cross-editor formatting rules
├── .env.example                 # Environment variables template
├── docker-compose.yml           # Multi-container setup
└── README.md                    # User documentation
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Server ports
BACKEND_PORT=8080
FRONTEND_PORT=3000

# SMTP (for email notifications)
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=user@example.com
SPRING_MAIL_PASSWORD=password

# Scanner settings
APP_SCANNER_MAX_CONCURRENCY=12
APP_MARKET_UNIVERSE_MAX_SYMBOLS=0

# Scheduler (cron-based automated jobs)
APP_SCHEDULER_HOURLY_ENABLED=true
APP_SCHEDULER_DAILY_ENABLED=true

# Notifications
APP_NOTIFICATION_RECIPIENTS=you@example.com
APP_NOTIFICATION_FROM=alerts@yourdomain.com
```

## IntelliJ IDEA Setup

The project includes proper `.idea/` configuration for IntelliJ IDEA:

1. Open the project root in IntelliJ IDEA
2. It will auto-detect:
   - Maven backend module
   - Node.js frontend module
   - JDK 25 requirement

### Run Configurations
- **Backend: Spring Boot** - Pre-configured to run `mvn spring-boot:run` from backend
- **Frontend: npm dev** - Can be added in IntelliJ npm panel

## Git Workflow

- **Main branch**: `main` (stable production-ready code)
- **Feature branches**: `feature/*` for new features
- **Bug fixes**: `fix/*` for bug fixes
- **Release branches**: `release/*` for release preparation

All commits should have clear, descriptive messages.

## Database

### SQLite Schema
The backend creates tables on first run:
- `watchlists` - Watchlist definitions
- `watchlist_stocks` - Stocks in each watchlist
- `scanner_runs` - Historical scan records
- `scanner_results` - Individual stock scan results
- `strategy_scanner_failures` - Failed scan diagnostics

Data location: `./backend/data/signals.db`

## API Endpoints

### Dashboard
- `GET /api/dashboard?strategy=sma|v20|multibagger` - Get strategy results

### Scanner Control
- `POST /api/scanner/run?strategy=sma|v20|multibagger` - Run a scan
- `POST /api/scanner/run?strategy=v20&group=V40%20NEXT` - Run scan for specific group

### Watchlist Management
- `GET /api/watchlists` - List all watchlists
- `POST /api/watchlists/replace` - Update watchlist

### Admin Notifications
- `POST /api/notifications/send-summary?mode=hourly|daily` - Send email notification

### Health Check
- `GET /api/health` - Server health status

## Common Tasks

### Adding a New Watchlist
1. Create YAML file in `backend/src/main/resources/watchlists/`
2. Register in `application.yml` under `app.watchlists.resources`
3. Rebuild and restart

### Extending Scanner Strategies
1. Add new `Strategy` enum value
2. Implement scanning logic in `ScannerService`
3. Add API endpoint in `ScannerController`

### Deploying to OCI
See README.md "Deploy On Oracle Cloud Always Free" section for full deployment steps.

## Testing

### Backend
```bash
cd backend
mvn test
```

### Frontend
```bash
cd frontend
npm test
```

## Code Quality Tools

### Frontend Linting & Formatting
```bash
cd frontend
npm run lint        # Check for linting errors
npm run lint:fix    # Auto-fix linting issues
npm run format      # Format code with Prettier
npm run format:check # Check formatting
```

### Backend
- Static analysis tools can be added to Maven plugins
- Tests should follow Spring Boot testing patterns

## Troubleshooting

### Port Already in Use
- Backend (8080): `lsof -i :8080 | grep LISTEN | kill -9 <PID>`
- Frontend (5173): `lsof -i :5173 | grep LISTEN | kill -9 <PID>`

### Docker Issues
```bash
# Clean up containers and volumes
docker compose down -v

# Rebuild images
docker compose build --no-cache

# Check logs
docker compose logs -f [service-name]
```

### IDE Not Recognizing Modules
- Reload Maven project: right-click `pom.xml` → Maven → Reload Project
- Invalidate cache: File → Invalidate Caches

## Additional Resources

- README.md - User documentation and deployment guide
- research/indian_multibagger_strategy.md - Stock screening methodology
- tradingview/ - TradingView Pine script references
