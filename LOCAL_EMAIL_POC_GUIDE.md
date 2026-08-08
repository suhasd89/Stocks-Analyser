# Local Email POC (Proof of Concept) Guide

## Overview

The application now supports **two email modes** for maximum flexibility during development and testing:

1. **Local Mode** (`local`) - Saves emails as HTML files for testing
2. **SMTP Mode** (`smtp`) - Sends real emails via SMTP provider

This guide covers the **Local Email POC** feature.

---

## 🎯 Local Email Mode Features

### What It Does
- **Saves emails** as `.html` files in `./data/emails/` directory
- **No SMTP required** - Works offline, no email credentials needed
- **Full preview** - Open HTML file in browser to see the email exactly as recipients would see it
- **Perfect for testing** - Verify email content, formatting, and data without sending
- **Console logging** - Detailed logs show what would have been sent

### Why It's Useful
✅ **Development**: Test email formatting before going to production
✅ **Testing**: Verify all alert data is correctly formatted
✅ **Debugging**: Check email content without needing SMTP
✅ **CI/CD**: Run tests without email infrastructure
✅ **Offline**: No internet or SMTP server needed

---

## 🚀 Quick Start

### Step 1: Update `application.yml`
```yaml
app:
  email:
    provider: local  # Enable local POC mode
```

### Step 2: Start the Application
```bash
docker compose up --build
# OR
mvn spring-boot:run  # backend only
```

### Step 3: Trigger the Email
```bash
# Test endpoint (recommended for testing)
curl -X POST http://localhost:8080/api/test/email/intraday

# OR production endpoint
curl -X POST http://localhost:8080/api/notifications/send-intraday
```

### Step 4: Check the Email
```bash
# List all emails
ls -lah ./data/emails/

# Open in browser
open ./data/emails/email_2026-08-05_10-30-45-123.html
```

---

## 📂 Directory Structure

```
project-root/
├── data/
│   ├── emails/                    # Local POC emails saved here
│   │   ├── email_2026-08-05_10-30-45-123.html
│   │   ├── email_2026-08-05_10-31-15-456.html
│   │   └── ... (more emails)
│   ├── signals.db                 # SQLite database
│   └── stock-tracker.log          # Application logs
```

---

## 🔧 Configuration

### In `application.yml`

**For Local Testing (POC)**:
```yaml
app:
  email:
    provider: local  # Saves to ./data/emails/
  notifications:
    recipients: your@email.com,second@email.com
    from: alerts@example.com
    sender-name: Stock Signal Tracker
```

**For Production (SMTP)**:
```yaml
app:
  email:
    provider: smtp  # Use SMTP
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your@gmail.com
    password: app-password
```

### No `.env` File Needed!
All configuration is now in `application.yml`. Just modify that file and restart.

---

## 📊 Example Local Email File

**Filename**: `email_2026-08-05_10-30-45-123.html`

**Content Structure**:
```html
<!DOCTYPE html>
<html>
<head>...</head>
<body>
  <!-- Metadata Section -->
  <div class="metadata">
    <h2>📧 Local Email (POC)</h2>
    From: Stock Signal Tracker <alerts@example.com>
    To: your@email.com
    Subject: 📈 Stock Alerts | 10:30 IST | 5 Active Signals
    Timestamp: 2026-08-05 10:30:45.123
    File: email_2026-08-05_10-30-45-123.html
  </div>
  
  <!-- Email Content -->
  <div class="email-content">
    [Full HTML email content with tables, styling, etc.]
  </div>
</body>
</html>
```

---

## 🧪 Testing Workflow

### 1. Development Phase

**Start with Local Mode**:
```yaml
app:
  email:
    provider: local
```

**Test the endpoint**:
```bash
curl -X POST http://localhost:8080/api/test/email/intraday
```

**Console output** (example):
```
═══════════════════════════════════════════════════════════════
📧 LOCAL EMAIL POC - Email saved successfully
───────────────────────────────────────────────────────────────
From: Stock Signal Tracker <alerts@example.com>
To: [your@email.com, second@email.com]
Subject: 📈 Stock Alerts | 10:30 IST | 5 Active Signals
File: ./data/emails/email_2026-08-05_10-30-45-123.html
Timestamp: 2026-08-05 10:30:45.123
───────────────────────────────────────────────────────────────
✓ Email saved to: /Users/suhasdeshmukh/Projects/personal/Stocks Analyser/data/emails/email_2026-08-05_10-30-45-123.html
✓ Open this file in a browser to preview the email
═══════════════════════════════════════════════════════════════
```

**Verify email**:
1. Open the HTML file in browser
2. Check formatting, colors, data
3. Verify recipient list, subject, sender

### 2. Switching to SMTP (Production)

Once satisfied with local testing:

```yaml
app:
  email:
    provider: smtp  # Switch to SMTP
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your@gmail.com
    password: app-password
```

**Restart application** and test:
```bash
curl -X POST http://localhost:8080/api/notifications/send-intraday
```

**Check email inbox** for the real email.

---

## 🔄 Email Provider Details

### LocalEmailService (POC)
```java
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "local")
public class LocalEmailService implements EmailService
```

**Features**:
- Creates HTML file with metadata section
- Includes sender, recipients, subject, timestamp
- Styled metadata section for easy reading
- Self-contained HTML (no external dependencies)
- Saves to `./data/emails/` directory
- Logs file path to console

### SmtpEmailService (Production)
```java
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailService implements EmailService
```

**Features**:
- Sends real emails via SMTP
- Uses Spring Mail sender
- Supports TLS/SSL
- Requires SMTP credentials
- Default when `app.email.provider` not specified

---

## 📋 Testing Checklist

### Local Email Tests

- [ ] Email file created in `./data/emails/`
- [ ] Filename has correct timestamp format
- [ ] HTML file opens in browser without errors
- [ ] Metadata section displays correctly
- [ ] Sender name and email visible
- [ ] All recipients listed
- [ ] Subject line correct
- [ ] Email body renders properly
- [ ] Tables formatted correctly
- [ ] Color coding applied (green/red/amber)
- [ ] Potential gain percentages calculated
- [ ] Currency formatting correct (Rs XX.XX)
- [ ] All watchlist groups present (V40, V40 NEXT, etc.)
- [ ] Console logs show success message

### SMTP Email Tests

- [ ] Email received in inbox
- [ ] Email subject line correct
- [ ] Email formatting matches local preview
- [ ] All data visible and correctly formatted
- [ ] Links work (if any)
- [ ] Responsive design works on mobile

---

## 🐛 Troubleshooting

### Email File Not Created?

**Check configuration**:
```bash
# Verify app.email.provider is set to 'local'
grep -A 2 "email:" ./backend/src/main/resources/application.yml
```

**Check directory permissions**:
```bash
# Create directory if missing
mkdir -p ./data/emails

# Check permissions
ls -ld ./data/emails
chmod 755 ./data/emails
```

**Check logs**:
```bash
# Look for email-related logs
docker compose logs backend | grep -i "local\|email\|poc"
```

### Email File Empty or Broken?

**Verify recipient configuration**:
```yaml
app:
  notifications:
    recipients: your@email.com  # Must have at least one
    from: alerts@example.com     # Must not be empty
```

**Check for errors**:
```bash
docker compose logs backend | grep -i error
```

### Switch Between Modes Isn't Working?

**Restart application after changing provider**:
```bash
# Stop
docker compose down

# Update application.yml
# Change: provider: local   →  provider: smtp

# Start
docker compose up --build
```

**Verify active provider** in logs:
```bash
docker compose logs backend | grep -i "email\|local\|smtp"
```

---

## 📝 Console Output Examples

### Successful Local Email

```
═══════════════════════════════════════════════════════════════
📧 LOCAL EMAIL POC - Email saved successfully
───────────────────────────────────────────────────────────────
From: Stock Signal Tracker <alerts@example.com>
To: 
  your@email.com
  second@email.com
Subject: 📈 Stock Alerts | 10:30 IST | 5 Active Signals
File: email_2026-08-05_10-30-45-123.html
Timestamp: 2026-08-05 10:30:45.123
───────────────────────────────────────────────────────────────
✓ Email saved to: /path/to/data/emails/email_2026-08-05_10-30-45-123.html
✓ Open this file in a browser to preview the email
═══════════════════════════════════════════════════════════════
```

### No Alerts to Send

```
[INFO] No active alerts found for intraday email. Skipping.
```

### Missing Configuration

```
[WARN] No notification recipients configured. Skipping intraday email.
[WARN] Notification sender address is not configured. Skipping intraday email.
```

---

## 🎨 HTML Email Preview

When you open the saved HTML file in a browser, you'll see:

```
┌─────────────────────────────────────────────────┐
│  📧 Local Email (POC)                            │
│                                                  │
│  From: Stock Signal Tracker <alerts@...>        │
│  To:   your@email.com                           │
│         second@email.com                        │
│                                                  │
│  Subject: 📈 Stock Alerts | 10:30 IST | 5 ...  │
│  Timestamp: 2026-08-05 10:30:45.123            │
│  File: email_2026-08-05_10-30-45-123.html      │
└─────────────────────────────────────────────────┘

[Beautiful email content below...]

┌─────────────────────────────────────────────────┐
│ 📈 Stock Signal Alerts                           │
│ 05/08/2026 10:30 IST                           │
│ 5 Active Signals                                │
└─────────────────────────────────────────────────┘

V40
┌──────┬───────────┬────────┬──────────┬─────────┐
│Symbol│ Strategy  │ Signal │ Entry    │ Current │
├──────┼───────────┼────────┼──────────┼─────────┤
│ TCS  │ SMA       │ BUY    │ Rs 3500  │ Rs3510  │
│ INFY │ V20       │ ALERT  │ Rs 1800  │ Rs1820  │
└──────┴───────────┴────────┴──────────┴─────────┘

[More sections for V40 NEXT, V200, BANK, NBFC...]
```

---

## 🔗 Related Endpoints

### Test Endpoints

```bash
# Get email info and available endpoints
GET /api/test/email-info

# Test intraday email (local or SMTP based on config)
POST /api/test/email/intraday

# Response:
# {
#   "ok": true,
#   "message": "Intraday email test triggered successfully",
#   "info": "Check console logs and ./data/emails/ directory for the generated email"
# }
```

### Production Endpoints

```bash
# Trigger intraday email (automatic every 30 mins)
POST /api/notifications/send-intraday

# Trigger hourly/daily summary
POST /api/notifications/send-summary?mode=hourly
POST /api/notifications/send-summary?mode=daily
```

---

## 🚀 Workflow Summary

```
Development          Testing           Production
     ↓                  ↓                   ↓
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  Local Email │ │ Verify Email │ │  SMTP Email  │
│  (POC Mode)  │ │   Preview    │ │  (Real Send) │
│   ./data/    │ │  in Browser  │ │ Inbox Check  │
│   emails/    │ └──────────────┘ └──────────────┘
└──────────────┘
    ↓
Check Console
Logs
    ↓
Verify File
Created
    ↓
Open in
Browser
    ↓
Review
Formatting
```

---

## 💡 Pro Tips

1. **Batch Testing**: Generate 10-15 emails quickly by calling the endpoint multiple times
2. **Compare Modes**: Generate same email in both local and SMTP modes to verify they match
3. **Team Testing**: Share HTML files with team members for review before production
4. **Version Control**: Don't commit email files - they're in `.gitignore`
5. **Archive Old Emails**: Occasionally clean old emails from `./data/emails/` to save space

---

## 🔄 Switching Between Modes at Runtime

The system uses Spring's `@ConditionalOnProperty` to automatically select the email service:

**application.yml**:
```yaml
app:
  email:
    provider: local  # Change this
```

**Available values**:
- `local` → LocalEmailService (saves HTML files)
- `smtp` → SmtpEmailService (sends real emails, default if not specified)

**Automatic Selection**:
- If `local` → uses LocalEmailService
- If `smtp` (or not specified) → uses SmtpEmailService

Just change the value and restart!

---

## 📞 Support

**Issues with local email?**
1. Check `./data/emails/` directory exists
2. Verify file permissions: `chmod 755 ./data/emails`
3. Check console logs for error messages
4. Look for files created with timestamp in filename

**Issues with SMTP?**
1. Verify SMTP credentials in `application.yml`
2. Check network connectivity to SMTP host
3. Verify firewall allows SMTP port
4. Check email logs in `./data/stock-tracker.log`

---

## 🎓 Learning Resources

- **Email Service Architecture**: See `EmailService.java`
- **Local Implementation**: See `LocalEmailService.java`
- **SMTP Implementation**: See `SmtpEmailService.java`
- **Integration**: See `IntradayEmailSchedulerService.java`

---

**Ready to test emails locally?** 🎉

Start with `provider: local` in `application.yml` and trigger emails to see them saved as beautiful HTML files!
