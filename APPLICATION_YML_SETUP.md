# Application Configuration Guide (application.yml)

## Overview

**All application configuration is now in `application.yml`** instead of scattered `.env` variables.

✅ **Benefits**:
- Single source of truth for all settings
- No `.env` file needed
- Better organization and documentation
- Environment-specific configurations possible
- Production-ready setup

---

## 📁 Configuration File Location

```
backend/src/main/resources/application.yml
```

---

## 🎯 Quick Configuration for Local Testing

### 1. **Local Email POC Mode** (Recommended for Testing)

Edit `backend/src/main/resources/application.yml`:

```yaml
app:
  email:
    provider: local  # 👈 Saves emails to ./data/emails/ (no SMTP needed)
  notifications:
    recipients: your@email.com
    from: alerts@example.com
    sender-name: Stock Signal Tracker
```

**Test it**:
```bash
curl -X POST http://localhost:8080/api/test/email/intraday
# Check ./data/emails/ for saved HTML file
```

---

### 2. **Production Email Mode** (SMTP)

Edit `backend/src/main/resources/application.yml`:

```yaml
app:
  email:
    provider: smtp  # 👈 Sends real emails
  notifications:
    recipients: your@email.com
    from: alerts@example.com
    sender-name: Stock Signal Tracker

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your@gmail.com
    password: app-password
```

**Gmail Setup**:
1. Enable 2-factor authentication
2. Generate app-specific password
3. Use that password above

---

## 📋 Complete Configuration Reference

### Server Settings

```yaml
server:
  port: 8080  # Backend port
```

### Logging

```yaml
logging:
  file:
    name: ./data/stock-tracker.log  # Log file location
  level:
    com.suhas.stocktracker: INFO    # Log level
    root: WARN                       # Root logger level
```

### Spring Boot Settings

```yaml
spring:
  threads:
    virtual:
      enabled: true  # Java 25 virtual threads

  datasource:
    url: jdbc:sqlite:./data/signals.db
    driver-class-name: org.sqlite.JDBC

  mail:
    host: smtp.gmail.com
    port: 587
    username: your@gmail.com
    password: your-app-password
    default-encoding: UTF-8
    properties:
      mail:
        debug: false
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 10000
          timeout: 10000
          writetimeout: 10000
```

### Email Provider (NEW)

```yaml
app:
  email:
    provider: local  # Options: 'local' or 'smtp'
    # local = File-based POC (saves to ./data/emails/)
    # smtp  = Real SMTP sending (default if not specified)
```

### Scanner Settings

```yaml
app:
  scanner:
    range: 2y                          # Historical range to analyze
    interval: 1d                       # Candle interval
    strategy-name: "SMA Strategy: 20/50/200"
    pause-millis: 350                  # Pause between API calls
    max-concurrency: 12                # Virtual thread pool size
```

### Market Universe Settings

```yaml
app:
  market-universe:
    nse-equity-list-url: https://archives.nseindia.com/content/equities/EQUITY_L.csv
    max-symbols: 0  # 0 = all symbols, or set limit (e.g., 100 for testing)
```

### Watchlist Settings

```yaml
app:
  watchlists:
    resources:
      V40: watchlists/v40.yml
      V40 NEXT: watchlists/v40-next.yml
      V200: watchlists/v200.yml
      BANK: watchlists/bank.yml
      NBFC: watchlists/nbfc.yml
```

### Scheduler Settings

```yaml
app:
  scheduler:
    timezone: Asia/Kolkata  # IST
    hourly:
      enabled: false
      cron: 0 5 10-15 ? * MON-FRI  # 10:05 AM - 3:05 PM
    daily:
      enabled: false
      cron: 0 20 16 ? * MON-FRI    # 4:20 PM
    intraday:
      # Automatic: Every 30 mins, 9 AM - 4 PM (no config needed)
      # Cron: 0 */30 9-15 ? * MON-FRI
```

### Notification Settings

```yaml
app:
  notifications:
    # Recipients: comma-separated emails (no spaces)
    recipients: your@email.com,second@email.com
    
    # Sender email
    from: alerts@example.com
    
    # Display name in emails
    sender-name: Stock Signal Tracker
    
    # Include empty alerts in summaries?
    hourly-include-empty: false   # Don't send if no alerts
    daily-include-empty: true     # Always send daily
```

---

## 🔄 Configuration Profiles

### Development (Local Testing)

```yaml
# application.yml (default)
app:
  email:
    provider: local  # No SMTP needed
  notifications:
    recipients: your@email.com
    from: alerts@example.com
```

### Staging (Testing with Real Email)

```yaml
# application.yml
app:
  email:
    provider: smtp
spring:
  mail:
    host: smtp.gmail.com
    username: staging@example.com
    password: staging-password
app:
  notifications:
    recipients: testing@example.com
    from: staging@example.com
```

### Production (Live)

```yaml
# application.yml
app:
  email:
    provider: smtp
spring:
  mail:
    host: smtp.provider.com
    username: production@example.com
    password: production-password
app:
  notifications:
    recipients: alerts@client.com
    from: no-reply@yourdomain.com
```

---

## 🚀 Switching Between Configurations

### Option 1: Edit application.yml Directly

```bash
# Edit the file
vim backend/src/main/resources/application.yml

# Change provider: local  →  provider: smtp

# Restart application
docker compose restart backend
```

### Option 2: Use Environment Variables (Docker)

```bash
# docker-compose.yml
environment:
  - APP_EMAIL_PROVIDER=local
  - SPRING_MAIL_HOST=smtp.gmail.com
  - SPRING_MAIL_USERNAME=your@gmail.com
  - SPRING_MAIL_PASSWORD=app-password
```

---

## 📝 Gmail SMTP Setup

### Step 1: Enable 2-Factor Authentication
1. Go to myaccount.google.com
2. Click "Security" in left menu
3. Enable 2-Step Verification

### Step 2: Generate App Password
1. Search for "App passwords" in Google Account
2. Select app: "Mail"
3. Select device: "Other (custom name)" → "Spring Boot"
4. Copy the generated 16-character password

### Step 3: Update application.yml
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your@gmail.com
    password: xxxx xxxx xxxx xxxx  # Paste 16-char password (without spaces)
```

---

## 🧪 Test Endpoints

### Get Email Info
```bash
GET /api/test/email-info

Response:
{
  "ok": true,
  "message": "Email testing endpoints available",
  "endpoints": {
    "test_intraday": "POST /api/test/email/intraday",
    "trigger_intraday": "POST /api/notifications/send-intraday"
  },
  "info": "Use these endpoints to test email sending. Check ./data/emails/ for local POC emails."
}
```

### Test Intraday Email (Local or SMTP)
```bash
POST /api/test/email/intraday

# If local mode: Email saved to ./data/emails/email_*.html
# If SMTP mode: Email sent to configured recipients
```

### Trigger Intraday Email (Production)
```bash
POST /api/notifications/send-intraday

# Same as above but production endpoint
# Normally runs automatically every 30 minutes
```

---

## 🔍 Verification

### Check Current Configuration

**In logs** (first 100 lines on startup):
```bash
docker compose logs backend | head -100
```

**Look for**:
```
[INFO] app.email.provider: local
[INFO] app.notifications.recipients: your@email.com
[INFO] app.notifications.from: alerts@example.com
```

### Verify Email Provider is Active

```bash
# Local mode should show:
docker compose logs backend | grep -i "local email"

# SMTP mode should show:
docker compose logs backend | grep -i "smtp"
```

---

## 📊 Configuration Sources (Priority)

1. **Environment Variables** (highest priority)
   ```bash
   export APP_EMAIL_PROVIDER=local
   ```

2. **Docker .env file** (docker-compose)
   ```bash
   APP_EMAIL_PROVIDER=local
   ```

3. **application.yml** (default)
   ```yaml
   app:
     email:
       provider: local
   ```

4. **Default values** (if not specified anywhere)
   ```yaml
   # Defaults defined in application.yml
   ```

---

## 🆘 Troubleshooting

### Configuration Not Taking Effect?

**Cause**: Application not restarted after config change

**Solution**:
```bash
# Stop
docker compose down

# Restart
docker compose up --build
```

### Email Provider Not Switching?

**Verify in logs**:
```bash
docker compose logs backend | grep -i "email\|provider"
```

**Expected output** (local mode):
```
LocalEmailService
```

**Expected output** (SMTP mode):
```
SmtpEmailService
```

### Missing Email Directory?

**Create it**:
```bash
mkdir -p ./data/emails
chmod 755 ./data/emails
```

---

## 💡 Pro Tips

1. **Local Testing First**: Always start with `provider: local`
2. **Verify in Browser**: Open saved HTML files to verify formatting
3. **Test Before Production**: Use staging SMTP settings first
4. **Document Changes**: Add comments when modifying `application.yml`
5. **Version Control**: Commit `application.yml` to git (contains no secrets)

---

## 🔐 Security Notes

✅ **Safe to commit to git**:
- `application.yml` (contains no secrets)
- Default values only

❌ **Never commit**:
- `.env` files with secrets
- SMTP passwords in plain text

**Best Practice**:
- Use environment variables in `docker-compose.yml` for secrets
- Use `.env` only for Docker (git ignored)
- Keep `application.yml` generic with placeholder values

---

## 🎓 Learning Resources

**Configuration Hierarchy**:
```
Environment Variables
        ↓
Docker .env
        ↓
application.yml
        ↓
Hardcoded Defaults
```

**Spring Boot Configuration**:
- Externalized configuration support
- Profile-specific configurations possible
- Environment variable interpolation with `${}`

---

## 📞 Quick Reference

```yaml
# LOCAL EMAIL (Testing)
app:
  email:
    provider: local

# PRODUCTION EMAIL (SMTP)
app:
  email:
    provider: smtp
spring:
  mail:
    host: smtp.gmail.com
    username: your@gmail.com
    password: app-password

# SCHEDULER
app:
  scheduler:
    timezone: Asia/Kolkata
    hourly:
      enabled: true
    daily:
      enabled: true

# NOTIFICATIONS
app:
  notifications:
    recipients: email@example.com
    from: alerts@example.com
```

---

**Ready to configure?** 🚀

Start with local mode in `application.yml` and test email sending with the POC service!
