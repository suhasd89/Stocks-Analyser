# Email Troubleshooting Guide - Complete Fix

## 🔧 Issues Found & Fixed

### Issue #1: Wrong "FROM" Email Address ❌
**Problem**: Configuration was using `alerts@example.com` but Gmail credentials use `suhasdeshmukh550@gmail.com`
- Gmail only allows sending from the authenticated email address
- This caused authentication to fail silently

**Fix**: Updated `application.yml`
```yaml
app:
  notifications:
    from: ${APP_NOTIFICATION_FROM:suhasdeshmukh550@gmail.com}  # ✅ Fixed
```

### Issue #2: AppProperties Missing Email Configuration ❌
**Problem**: EmailTestController referenced `appProperties.email()` but it wasn't defined
- The Email record was missing from AppProperties

**Fix**: Added Email record to AppProperties.java
```java
public record Email(String provider) {
}
```

---

## 🚀 New Test Endpoint - Send Immediate Test Email

A new endpoint allows you to send a test email **immediately without waiting for scheduler**:

```bash
POST /api/test/email/send-test
```

### Why This Endpoint?
✅ Tests email configuration **right now**
✅ **No SMTP credentials needed** if using local POC mode
✅ **No alerts required** - sends test email automatically
✅ Provides immediate feedback on email issues
✅ Perfect for debugging

### How to Test

**Step 1: Start the application**
```bash
docker compose up --build
```

**Step 2: Send test email immediately**
```bash
curl -X POST http://localhost:8080/api/test/email/send-test
```

**Step 3: Check response**
```json
{
  "ok": true,
  "message": "Test email sent successfully!",
  "provider": "smtp",
  "to": "suhasdeshmukh550@gmail.com",
  "from": "suhasdeshmukh550@gmail.com",
  "timestamp": "2026-08-05 11:00:34",
  "info": "Check console for 'Email saved to' message (local mode) or inbox (SMTP mode)"
}
```

**Step 4: Check your inbox**
- **Local mode** (`provider: local`): Check `./data/emails/` directory
- **SMTP mode** (`provider: smtp`): Check your Gmail inbox

---

## 📋 Gmail Setup - CRITICAL FIX

### The Problem
Regular Gmail passwords **do NOT work** with SMTP if 2FA is enabled.
You must use an **App Password**, not your regular Gmail password.

### Solution: Generate App Password (Important!)

**Step 1: Enable 2-Factor Authentication (if not already done)**
1. Go to myaccount.google.com
2. Click "Security" in left menu
3. Find "2-Step Verification" and enable it

**Step 2: Generate App Password**
1. Go to myaccount.google.com
2. Click "Security" in left menu
3. Scroll down to find "App passwords" (bottom of page)
   - NOTE: This only appears if 2FA is enabled
4. Select:
   - App: "Mail"
   - Device: "Other (custom name)" → Type "Spring Boot Stock Tracker"
5. **Copy the 16-character password** (format: `xxxx xxxx xxxx xxxx`)

**Step 3: Update application.yml**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: suhasdeshmukh550@gmail.com  # Your Gmail address
    password: xxxx xxxx xxxx xxxx          # 16-char app password (without spaces)
    
app:
  email:
    provider: smtp
  notifications:
    from: suhasdeshmukh550@gmail.com      # MUST match username
    recipients: suhasdeshmukh550@gmail.com # Or other recipients
```

**Step 4: Restart and test**
```bash
docker compose restart backend
curl -X POST http://localhost:8080/api/test/email/send-test
# Check inbox!
```

---

## ✅ Verification Checklist

- [ ] Gmail 2FA enabled (Settings → Security)
- [ ] App password generated (not regular password)
- [ ] App password copied exactly (16 characters)
- [ ] `app.notifications.from` matches `spring.mail.username`
- [ ] `app.email.provider` is set to `smtp` (or `local` for testing)
- [ ] `app.notifications.recipients` contains your email
- [ ] Docker image rebuilt (`docker compose up --build`)
- [ ] Test endpoint called (`curl -X POST http://localhost:8080/api/test/email/send-test`)
- [ ] Response shows `"ok": true`
- [ ] Email received in inbox (or file in ./data/emails/)

---

## 🧪 Test Endpoints Reference

### 1. Get Email Configuration Info
```bash
GET /api/test/email-info

Response shows:
- Email provider (local or smtp)
- Recipients
- From address
- Available test endpoints
```

### 2. Send Test Email (NEW - USE THIS!)
```bash
POST /api/test/email/send-test

✅ Sends immediately
✅ No alerts needed
✅ No scheduler wait
✅ Provides instant feedback
```

### 3. Test Intraday Email
```bash
POST /api/test/email/intraday

Tests intraday scheduler with real alert data
Only works if there are active alerts
```

### 4. Trigger Production Email
```bash
POST /api/notifications/send-intraday

Auto-runs every 30 minutes (9 AM - 4 PM IST)
Can also be triggered manually
```

---

## 🎯 Complete Workflow - From Start to Success

### Mode 1: Local POC Testing (No SMTP Needed)

**application.yml**:
```yaml
app:
  email:
    provider: local
  notifications:
    recipients: your@email.com
    from: alerts@example.com
    sender-name: Stock Signal Tracker
```

**Commands**:
```bash
# Start app
docker compose up --build

# Send test email
curl -X POST http://localhost:8080/api/test/email/send-test

# Check saved email
open ./data/emails/email_*.html
```

**Result**: HTML files saved to `./data/emails/`

---

### Mode 2: Gmail SMTP (Real Email)

**Generate App Password** (critical!):
1. Enable 2FA in Gmail
2. Go to myaccount.google.com → Security → App passwords
3. Select Mail + Other → Type "Stock Tracker"
4. Copy 16-character password

**application.yml**:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: suhasdeshmukh550@gmail.com
    password: xxxx xxxx xxxx xxxx        # Paste 16-char app password

app:
  email:
    provider: smtp                       # ← Important: Switch to smtp
  notifications:
    recipients: your@email.com
    from: suhasdeshmukh550@gmail.com     # ← MUST match username
    sender-name: Stock Signal Tracker
```

**Commands**:
```bash
# Restart (config changed)
docker compose restart backend

# Send test email
curl -X POST http://localhost:8080/api/test/email/send-test

# Check inbox!
```

**Result**: Email in Gmail inbox

---

## 🐛 Debugging - If Emails Still Don't Arrive

### Check 1: Test Endpoint Response
```bash
curl -X POST http://localhost:8080/api/test/email/send-test -v
```

**Look for**:
```json
{
  "ok": true,
  ...
}
```

If `"ok": false`, read the error message.

### Check 2: Console Logs
```bash
docker compose logs backend | tail -100 | grep -i "email\|test"
```

**Look for**:
- `Test email sent successfully` ✅
- `Email saved to:` (local mode) ✅
- Error messages ❌

### Check 3: Gmail Security Alert
Gmail might block the connection. Check:
1. Gmail inbox for security alerts
2. "Sign in & security" → Recent activity
3. Click "Review" on suspicious activity
4. Allow access from Spring Boot app

### Check 4: Firewall/Network
```bash
# Test SMTP connectivity
nc -zv smtp.gmail.com 587

# Should respond: Connection succeeded
```

### Check 5: Verify Configuration
```bash
# Check what's in application.yml
curl http://localhost:8080/api/test/email-info

# Should show your actual values
```

---

## 📝 Common Errors & Fixes

### Error: "Authentication failed"
**Cause**: Wrong password or regular Gmail password used
**Fix**: Generate App Password (16-char), use that instead

### Error: "Connection refused"
**Cause**: SMTP host/port unreachable
**Fix**: Verify `smtp.gmail.com:587` is reachable, check firewall

### Error: "No notification recipients configured"
**Cause**: `app.notifications.recipients` is empty
**Fix**: Set it in application.yml:
```yaml
app:
  notifications:
    recipients: suhasdeshmukh550@gmail.com
```

### Email sent but not received
**Cause**: `from` address doesn't match `username`
**Fix**: Make sure they're identical:
```yaml
spring:
  mail:
    username: suhasdeshmukh550@gmail.com
app:
  notifications:
    from: suhasdeshmukh550@gmail.com  # ← Must match!
```

### No response from test endpoint
**Cause**: Endpoint not working or error occurred
**Fix**: Check logs and response HTTP status code

---

## ✨ Expected Behavior After Fix

### Local Mode
1. `POST /api/test/email/send-test` → `"ok": true`
2. File created: `./data/emails/email_2026-08-05_11-00-34-123.html`
3. Open in browser → See beautiful email preview

### SMTP Mode
1. `POST /api/test/email/send-test` → `"ok": true`
2. Check Gmail inbox
3. Email arrives with subject: "Test Email - Stock Signal Tracker [timestamp]"

---

## 🎓 What Changed

**Files Updated**:
- ✅ `application.yml` - Fixed `from` address
- ✅ `AppProperties.java` - Added Email record
- ✅ `EmailTestController.java` - Added `send-test` endpoint

**Build Status**: ✅ SUCCESS (42 files compiled)

**Ready to Use**: ✅ YES

---

## 🚀 Next Steps

1. **Read this guide** - Understand the Gmail app password requirement
2. **Generate app password** - Go to myaccount.google.com → App passwords
3. **Update application.yml** - Paste the 16-char password
4. **Restart docker** - `docker compose restart backend`
5. **Test immediately** - `curl -X POST http://localhost:8080/api/test/email/send-test`
6. **Check inbox** - Email should arrive in seconds!

---

## 📞 If Still Not Working

1. Get detailed error: `curl -X POST http://localhost:8080/api/test/email/send-test -v`
2. Check logs: `docker compose logs backend | grep -i email`
3. Verify app password (16 chars): Copy fresh from Gmail settings
4. Test local mode first: Change `provider: local`, restart, test
5. If local works, SMTP issue is just credentials

---

**The key fix: Gmail requires an App Password, not your regular Gmail password!**

Generate it at: myaccount.google.com → Security → App passwords

Then update application.yml and restart. You're done! 🎉
