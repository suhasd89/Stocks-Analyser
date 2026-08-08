# Intraday Email Scheduler Guide

## Overview

A new **Intraday Email Scheduler** has been implemented that automatically sends visually striking stock alert emails every 30 minutes from **9:00 AM to 4:00 PM IST** (weekdays only).

## Features

### ✅ Automatic Scheduling
- **Frequency**: Every 30 minutes (9:00, 9:30, 10:00, 10:30, ..., 15:30)
- **Market Hours**: 9:00 AM to 4:00 PM IST
- **Trading Days**: Monday to Friday
- **Timezone**: Asia/Kolkata (IST)
- **No Configuration Needed**: Automatically enabled and runs in background

### ✅ Data Included
- **Entry Price**: Price at which the signal was generated
- **Current Price**: Latest market price
- **Exit Price**: Target price based on strategy
- **Potential Gains**: Percentage gain from current to target price
- **Signal Type**: BUY, SELL, or ALERT badges

### ✅ Organized by Watchlist
Data is automatically organized by watchlist group:
- **V40**: Large-cap stocks
- **V40 NEXT**: Mid-cap stocks
- **V200**: Broader market coverage
- **BANK**: Banking sector stocks
- **NBFC**: Non-banking financial companies

### ✅ Strategies Covered
- **SMA Strategy**: Simple Moving Average (SMA20/50/200)
- **V20 Strategy**: 20% momentum-based screener

> Note: Multibagger strategy is excluded from intraday emails (runs once daily)

### ✅ Beautiful HTML Formatting
- **Gradient Header**: Eye-catching purple gradient background
- **Color-Coded Signals**: Green (BUY), Red (SELL), Amber (ALERT)
- **Visual Indicators**: ▲ for gains, ▼ for losses
- **Responsive Design**: Works on desktop, tablet, mobile
- **Professional Layout**: Clean tables with hover effects

## Configuration

### Email Recipients
Set in `.env`:
```bash
APP_NOTIFICATION_RECIPIENTS=your.email@example.com,second.email@example.com
```

Multiple recipients: comma-separated, no spaces after commas

### Email Sender
Set in `.env`:
```bash
APP_NOTIFICATION_FROM=alerts@yourdomain.com
APP_NOTIFICATION_SENDER_NAME=Stock Signal Tracker
```

### Timezone (if not IST)
Set in `.env`:
```bash
APP_SCHEDULER_TIMEZONE=Asia/Kolkata  # Change if needed
```

### Disable Intraday Scheduler
The scheduler runs automatically. To disable it, you would need to:
1. Comment out the `@Scheduled` annotation in `IntradayEmailSchedulerService`
2. Rebuild the project
3. Restart the application

(Currently no environment variable to disable - add one if needed)

## Manual Trigger

### Via API
```bash
curl -X POST http://localhost:8080/api/notifications/send-intraday
```

Response:
```json
{
  "ok": true,
  "message": "Intraday email scheduled successfully"
}
```

### Via Docker
```bash
curl -X POST http://localhost:8080/api/notifications/send-intraday
```

### Via IntelliJ Terminal
While backend is running:
```bash
curl -X POST http://localhost:8080/api/notifications/send-intraday
```

## Email Content Example

### Subject Line
```
📈 Stock Alerts | 11:30 IST | 5 Active Signals
```

### Email Structure
```
Header: Stock Signal Alerts
Timestamp: 05/08/2026 11:30 IST
Summary: 5 Active Signals

[V40 Section]
V40
┌─────────────────────────────────────────────┐
│ Symbol │ Strategy │ Signal │ Entry │ Current │ Exit │ Potential Gain │
├─────────────────────────────────────────────┤
│ RELIANCE │ SMA │ BUY │ Rs 2500.00 │ Rs 2510.00 │ Rs 2600.00 │ ▲ 3.58% │
│ INFY │ V20 │ ALERT │ Rs 1800.00 │ Rs 1820.00 │ Rs 1900.00 │ ▲ 4.40% │
└─────────────────────────────────────────────┘

[V40 NEXT Section]
V40 NEXT
┌─────────────────────────────────────────────┐
│ Symbol │ Strategy │ Signal │ Entry │ Current │ Exit │ Potential Gain │
│ WIPRO │ SMA │ SELL │ Rs 420.00 │ Rs 415.00 │ Rs 400.00 │ ▼ -3.61% │
└─────────────────────────────────────────────┘

[V200 Section]
[Similar table structure]

[BANK Section]
[Similar table structure]

[NBFC Section]
[Similar table structure]

Footer:
Stock Signal Tracker - Intraday Alert System
Generated on 05/08/2026 11:30 IST
```

## Color Coding

### Signal Badges
- 🟢 **BUY** (Green): Strong buy signal
- 🔴 **SELL** (Red): Strong sell signal
- 🟠 **ALERT** (Amber): Caution/monitoring signal

### Potential Gains
- 🟢 **Positive** (Green): Expected profit from current to exit price
- 🔴 **Negative** (Red): Expected loss from current to exit price

## How It Works

### 1. Trigger Mechanism
```
Cron: 0 */30 9-15 ? * MON-FRI (Asia/Kolkata)
↓
Every 30 minutes, 9:00 AM - 3:30 PM
On weekdays only
```

### 2. Data Gathering
- Runs SMA scanner
- Runs V20 scanner
- Fetches latest dashboard data
- Filters for active alerts (BUY, SELL, ALERT)

### 3. Organization
- Groups results by watchlist (V40, V40 NEXT, V200, BANK, NBFC)
- Calculates potential gains
- Formats data for email

### 4. Email Generation
- Generates HTML email with embedded CSS
- Applies color coding and formatting
- Includes timestamp and signal count
- Responsive design for all devices

### 5. Delivery
- Sends to configured recipients
- Logs success/failure to application logs
- No alert: Silent skip (not sent if no active signals)

## Troubleshooting

### Email Not Sending?

**Check Configuration**
```bash
# Verify in .env
APP_NOTIFICATION_RECIPIENTS=your@email.com
APP_NOTIFICATION_FROM=alerts@domain.com
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_USERNAME=smtp.user
SPRING_MAIL_PASSWORD=smtp.password
```

**Check Logs**
```bash
# View application logs
docker compose logs backend | grep -i "intraday\|email"

# Or in IntelliJ console
# Look for: "Starting intraday email scheduler" or error messages
```

**Test SMTP**
```bash
# Manually trigger to test
curl -X POST http://localhost:8080/api/notifications/send-intraday
# Should see success message or error details in logs
```

**Common Issues**
- SMTP credentials incorrect → Check `.env` values
- TLS/SSL mismatch → Verify `SPRING_MAIL_SMTP_STARTTLS_ENABLE`
- Port blocked → Ensure SMTP port is accessible (usually 587)
- Invalid sender email → Not approved in OCI/Gmail/SendGrid
- Empty alerts → No signals detected (this is normal)

### No Alerts Sent?

This is **normal and expected**. The scheduler only sends email if:
1. At least one active alert exists (BUY, SELL, or ALERT)
2. Email is properly configured
3. SMTP connection is working

**To force a test**:
Use the manual trigger API and ensure you have test data with active signals.

### Timezone Issues

If emails show wrong time:
```bash
# Update .env
APP_SCHEDULER_TIMEZONE=Asia/Kolkata  # For IST
# or
APP_SCHEDULER_TIMEZONE=Asia/Kolkata  # Default timezone
```

Then restart the application.

## Performance Considerations

### Scan Impact
- Runs SMA and V20 scanners (can take 2-10 minutes depending on data)
- Uses existing scanner infrastructure
- Non-blocking: other API requests continue

### Email Sending
- Asynchronous after scan completes
- Does not block scheduler
- Typically <1 second to send

### Optimization
- Only scans SMA and V20 (not Multibagger)
- Filters for active alerts only
- Skips email if no alerts found

## Example Cron Schedule

**Every 30 minutes, 9 AM to 4 PM IST**:
```
9:00 AM  ✓ Email 1
9:30 AM  ✓ Email 2
10:00 AM ✓ Email 3
10:30 AM ✓ Email 4
11:00 AM ✓ Email 5
11:30 AM ✓ Email 6
12:00 PM ✓ Email 7
12:30 PM ✓ Email 8
1:00 PM  ✓ Email 9
1:30 PM  ✓ Email 10
2:00 PM  ✓ Email 11
2:30 PM  ✓ Email 12
3:00 PM  ✓ Email 13
3:30 PM  ✓ Email 14
4:00 PM  ✓ Email 15
4:30 PM  ✗ After market hours - SKIPPED
```

## Integration with Existing Schedulers

The intraday scheduler works alongside existing schedulers:

| Scheduler | Time | Frequency | Strategies | Type |
|-----------|------|-----------|-----------|------|
| **Intraday** | 9 AM - 4 PM | Every 30 min | SMA, V20 | Automatic |
| **Hourly** | 10:05 AM - 3:05 PM | Once per hour | All | Configurable |
| **Daily** | 4:20 PM | Once per day | All | Configurable |

All three run independently and don't interfere with each other.

## Future Enhancements

Possible improvements (can be added):
- [ ] Enable/disable via environment variable
- [ ] Customizable email template
- [ ] Different strategies for intraday emails
- [ ] SMS alerts in addition to email
- [ ] Webhook notifications
- [ ] Slack integration
- [ ] Customizable schedule (not just 30 mins)

## API Reference

### Send Intraday Alert
```
POST /api/notifications/send-intraday

Response:
{
  "ok": true,
  "message": "Intraday email scheduled successfully"
}
```

### Send Manual Summary
```
POST /api/notifications/send-summary?mode=hourly|daily

Response:
{
  "ok": true,
  "mode": "hourly",
  "message": "Sent hourly summary email to 1 recipient(s) with 5 active alert(s)."
}
```

## Logs to Monitor

### Success
```
[INFO] Starting intraday email scheduler at 05/08/2026 11:30 IST
[INFO] Sent intraday email to 1 recipient(s)
```

### No Alerts
```
[INFO] No active alerts found for intraday email. Skipping.
```

### Configuration Issues
```
[WARN] No notification recipients configured. Skipping intraday email.
[WARN] Notification sender address is not configured. Skipping intraday email.
```

### Errors
```
[ERROR] Error in intraday email scheduler: [exception details]
[ERROR] Failed to execute intraday email: [exception details]
```

## Testing

### Local Testing (Development)

1. **Set up SMTP locally** (using MailHog or similar):
   ```bash
   docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog
   ```

2. **Update `.env`**:
   ```bash
   SPRING_MAIL_HOST=localhost
   SPRING_MAIL_PORT=1025
   SPRING_MAIL_USERNAME=test
   SPRING_MAIL_PASSWORD=test
   APP_NOTIFICATION_FROM=test@localhost
   APP_NOTIFICATION_RECIPIENTS=you@example.com
   ```

3. **Restart backend** and trigger:
   ```bash
   curl -X POST http://localhost:8080/api/notifications/send-intraday
   ```

4. **View email** at `http://localhost:8025`

### Production Testing

1. **Verify credentials** in `.env`
2. **Set test recipient** to yourself
3. **Manual trigger**:
   ```bash
   curl -X POST http://your-server:8080/api/notifications/send-intraday
   ```
4. **Check mailbox** for receipt
5. **Review logs** for any errors

## Support

For issues or questions:
1. Check logs: `docker compose logs backend | grep -i email`
2. Verify SMTP configuration in `.env`
3. Test manual trigger API
4. Check email validity and SMTP provider settings
