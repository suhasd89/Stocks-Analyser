# Indian Stock Signal Tracker

This repo contains a Spring Boot microservice and a React UI for the stock tracker.

## Structure

- [backend](./backend): Spring Boot + Maven microservice running on Java 25
- [frontend](./frontend): React + Vite UI
- [tradingview](./tradingview): TradingView Pine scripts

## What the new stack does

- Loads watchlists from YAML resources configured in [application.yml](./backend/src/main/resources/application.yml)
- Runs local daily scanners against Yahoo Finance candles
- Runs Yahoo candle scans in parallel with bounded Java virtual-thread workers
- Stores watchlists in SQLite after an initial YAML seed
- Supports scheduled hourly and daily scanner emails through SMTP
- Supports three strategy pages:
  - `SMA`: `BUY` when `sma200 > sma50 > sma20 > close`, `SELL` when `close > sma20 > sma50 > sma200`
  - `V20`: sequence-based 20% move screener using your Pine logic
  - `Multibagger`: first-pass discovery screener over the NSE EQ universe using trend, momentum, 52-week strength, liquidity, and score-based reasoning
- Exposes APIs for dashboard data and scanner runs
- Exposes APIs for watchlist administration
- Exposes an admin-only API to manually send scheduled summaries
- Shows a React dashboard with:
  - `SMA` page
  - `V20` page
  - `Multibagger` page
  - active scanner alerts
  - scan issue diagnostics for failed Yahoo symbols from the latest run
  - dark/light theme toggle
  - watchlist manager for `V40`, `V40 Next`, `V200`, `Bank`, and `NBFC`
  - email / WhatsApp / copy sharing for the visible active alerts

## Backend APIs

- `GET /api/health`
- `GET /api/dashboard?strategy=sma`
- `GET /api/dashboard?strategy=v20`
- `GET /api/dashboard?strategy=multibagger`
- `POST /api/scanner/run?strategy=sma`
- `POST /api/scanner/run?strategy=v20`
- `POST /api/scanner/run?strategy=multibagger`
- `POST /api/scanner/run?strategy=v20&group=V40%20NEXT`
- `GET /api/watchlists`
- `POST /api/watchlists/replace`
- `POST /api/notifications/send-summary?mode=hourly`
- `POST /api/notifications/send-summary?mode=daily`

The notification endpoints are admin-only.

## Run backend

Typical local run on your machine:

```bash
cd backend
mvn spring-boot:run
```

The backend defaults to port `8080`. The project now targets Java `25` and Spring Boot `4.0.x`, so local Maven runs need JDK 25.

To enable scheduled email notifications locally, export SMTP and scheduler variables before starting:

```bash
export SPRING_MAIL_HOST="smtp.example.com"
export SPRING_MAIL_PORT="587"
export SPRING_MAIL_USERNAME="smtp-user"
export SPRING_MAIL_PASSWORD="smtp-password"
export APP_NOTIFICATION_FROM="alerts@yourdomain.com"
export APP_NOTIFICATION_RECIPIENTS="you@example.com"
export APP_SCHEDULER_HOURLY_ENABLED="true"
export APP_SCHEDULER_DAILY_ENABLED="true"
```

## Run frontend

Typical local run on your machine:

```bash
cd frontend
npm install
npm run dev
```

The frontend defaults to port `5173` and calls `http://localhost:8080` unless you override:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## Run with Docker

You can also run the full app with Docker Compose from the project root:

```bash
docker compose up --build
```

Then open:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/api/health`

Sign in, choose the `Multibagger` strategy tab, and click `Run Discovery Scan`. The scanner downloads the NSE EQ universe, scans those symbols through Yahoo daily candles, and shows matched candidates with score notes.

Useful Docker commands:

```bash
docker compose up --build -d
docker compose logs -f
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```

You can copy the sample environment file first:

```bash
cp .env.example .env
```

Notes:

- The frontend container serves the built React app through Nginx.
- Nginx proxies `/api/*` requests to the backend container.
- SQLite data is persisted in the Docker volume `backend-data`, so your scanner history remains even if the containers are recreated.
- If you want to reset the Docker database and reseed the YAML watchlists from scratch, run `docker compose down -v` and then `docker compose up --build`.
- Full NSE discovery can take time and may be throttled by Yahoo. For a smaller test run, set `APP_MARKET_UNIVERSE_MAX_SYMBOLS=100` in `.env`; set it to `0` or leave it unset for the full NSE EQ universe.

## Notes

- The backend uses browser-style headers when calling Yahoo Finance to reduce rate-limit issues.
- Scanner runs can be scoped by list using `group`, so you can scan only `V40`, `V40 NEXT`, `V200`, `BANK`, or `NBFC` instead of the whole strategy universe.
- The `multibagger` strategy is not backed by a static `multibagger.yml` file. It fetches NSE's EQ symbol list from `APP_MARKET_UNIVERSE_NSE_EQUITY_LIST_URL`, scans the universe, and labels names as `ALERT`, `WATCH`, or `NONE` with a 0-100 score and notes explaining why a stock matched.
- The multibagger screen is only the discovery layer. Treat every result as research input and verify sales/PAT acceleration, cash conversion, receivables, pledges, valuation, exchange filings, and source documents before acting.
- The scanner uses virtual threads with bounded concurrency. Tune `APP_SCANNER_MAX_CONCURRENCY` if Yahoo starts throttling or if scans are still too slow.
- The SQLite file is configured as `./data/signals.db` relative to the backend working directory.
- Application logs are written to `./data/stock-tracker.log` by default and also appear in Docker logs.
- Scanner failures are stored in SQLite in `strategy_scanner_failures` and shown in the `Scan Issues` page with symbol, list, Yahoo symbol, and error.
- Admin users can open `Scan Issues`, choose a failed row, edit the list/symbol/name/Yahoo symbol, and save that stock back into the database without replacing the whole watchlist.
- The repo is now Java + React only. All earlier Python prototype files have been removed.
- Watchlists are plug-and-play YAML files:
- YAML files now act as the initial seed for the database-backed watchlists:
  - [v40.yml](./backend/src/main/resources/watchlists/v40.yml)
  - [v40-next.yml](./backend/src/main/resources/watchlists/v40-next.yml)
  - [v200.yml](./backend/src/main/resources/watchlists/v200.yml)
  - [bank.yml](./backend/src/main/resources/watchlists/bank.yml)
  - [nbfc.yml](./backend/src/main/resources/watchlists/nbfc.yml)
- The source-verified multibagger research framework lives in [research/indian_multibagger_strategy.md](./research/indian_multibagger_strategy.md). The app implements the first-pass local screener from that framework; manual source verification remains outside the candle scanner.
- After first startup, you can replace a list directly from the UI by pasting one company per line into the `Watchlist Manager`.
- WhatsApp and email sharing are client-side convenience actions. WhatsApp opens with prefilled text, but the final send still happens from your device/app.
- Backend scheduled emails are server-side and use SMTP. For OCI Email Delivery, configure the SMTP values in `.env` and use an approved sender email in `APP_NOTIFICATION_FROM`.
- Reference Pine scripts available:
  - [tradingview/sma_strategy_20_50_200.pine](./tradingview/sma_strategy_20_50_200.pine)
  - [tradingview/v20.pine](./tradingview/v20.pine)
  - [tradingview/india_pine_screener.pine](./tradingview/india_pine_screener.pine): TradingView Pine Screener-ready NSE/BSE scanner using V20 momentum, trend, 52-week-high, and liquidity filters
  - [tradingview/india_pine_screener_usage.md](./tradingview/india_pine_screener_usage.md): setup notes for TradingView Pine Screener

## Scheduler Settings

The backend now supports two cron-based jobs:

- Hourly summary:
  - env: `APP_SCHEDULER_HOURLY_ENABLED`
  - cron env: `APP_SCHEDULER_HOURLY_CRON`
  - default cron: `0 5 10-15 ? * MON-FRI`
- Daily summary:
  - env: `APP_SCHEDULER_DAILY_ENABLED`
  - cron env: `APP_SCHEDULER_DAILY_CRON`
  - default cron: `0 20 16 ? * MON-FRI`

Both use:

- timezone env: `APP_SCHEDULER_TIMEZONE`
- default timezone: `Asia/Kolkata`

Notification env vars:

- `APP_NOTIFICATION_RECIPIENTS`
- `APP_NOTIFICATION_FROM`
- `APP_NOTIFICATION_SENDER_NAME`
- `APP_NOTIFICATION_HOURLY_INCLUDE_EMPTY`
- `APP_NOTIFICATION_DAILY_INCLUDE_EMPTY`

## Deploy On Oracle Cloud Always Free

This app is suitable for one OCI Always Free VM running Docker Compose.

### 1. Create the VM

In OCI Console:

1. Create a Compute instance in your home region
2. Use an Always Free shape, preferably `VM.Standard.A1.Flex`
3. Start with `1 OCPU / 6 GB RAM` or `2 OCPU / 12 GB RAM`
4. Use Ubuntu or Oracle Linux
5. Allow SSH ingress on port `22`
6. Allow app ingress on either:
   - `3000` for the frontend, or
   - `80` if you later place Nginx directly in front

### 2. Connect to the server

From your machine:

```bash
ssh -i /path/to/your-private-key opc@YOUR_OCI_PUBLIC_IP
```

On Ubuntu, install Docker:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
```

Log out once and SSH back in so the Docker group applies.

### 3. Copy the project

From your machine, navigate to the project directory and run:

```bash
scp -r -i /path/to/your-private-key . opc@YOUR_OCI_PUBLIC_IP:~/stock-tracker
```

Then on the OCI VM:

```bash
cd ~/stock-tracker
cp .env.example .env
```

Edit `.env` and fill in:

- SMTP host, port, username, password
- approved sender email in `APP_NOTIFICATION_FROM`
- your email in `APP_NOTIFICATION_RECIPIENTS`
- whether hourly and daily jobs should be enabled

### 4. Configure OCI Email Delivery

Use OCI Email Delivery if you want Oracle-managed email sending.

High-level setup:

1. Create an email domain in OCI
2. Configure SPF and DKIM for your domain
3. Create an approved sender
4. Generate SMTP credentials
5. Copy the SMTP host, username, and password into `.env`

Official docs:

- [OCI Email Delivery Overview](https://docs.oracle.com/en-us/iaas/Content/Email/Concepts/overview.htm)
- [OCI Email Delivery Getting Started](https://docs.oracle.com/en-us/iaas/Content/Email/Reference/gettingstarted.htm)

### 5. Start the app

On the OCI VM:

```bash
cd ~/stock-tracker
docker compose up --build -d
```

Then open:

- `http://YOUR_OCI_PUBLIC_IP:3000`

### 6. Test email manually

Log in as admin, then from the server or any API client with an admin session, trigger:

```bash
curl -X POST "http://localhost:8080/api/notifications/send-summary?mode=daily"
```

Or:

```bash
curl -X POST "http://localhost:8080/api/notifications/send-summary?mode=hourly"
```

### 7. Keep it running

Docker Compose is already configured with `restart: unless-stopped`, so the containers will come back after reboot if Docker starts.

Useful commands on OCI:

```bash
docker compose ps
docker compose logs -f
docker compose restart
docker compose down
```
