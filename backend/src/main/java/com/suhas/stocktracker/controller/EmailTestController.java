package com.suhas.stocktracker.controller;

import com.suhas.stocktracker.config.AppProperties;
import com.suhas.stocktracker.service.EmailService;
import com.suhas.stocktracker.service.IntradayEmailSchedulerService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {
    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);
    private final IntradayEmailSchedulerService intradayEmailSchedulerService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public EmailTestController(
        IntradayEmailSchedulerService intradayEmailSchedulerService,
        EmailService emailService,
        AppProperties appProperties
    ) {
        this.intradayEmailSchedulerService = intradayEmailSchedulerService;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    @GetMapping("/email-info")
    public Map<String, Object> getEmailInfo() {
        return Map.of(
            "ok", true,
            "message", "Email testing endpoints available",
            "email_provider", appProperties.email().provider(),
            "recipients", appProperties.notifications().recipients(),
            "from", appProperties.notifications().from(),
            "endpoints", Map.of(
                "send_test_email", "POST /api/test/email/send-test",
                "test_intraday", "POST /api/test/email/intraday",
                "trigger_intraday", "POST /api/notifications/send-intraday"
            ),
            "info", "Use /api/test/email/send-test to send a simple test email immediately"
        );
    }

    @PostMapping("/email/send-test")
    public Map<String, Object> sendTestEmail() {
        try {
            log.info("🧪 Sending simple test email...");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String htmlBody = buildTestEmailBody(timestamp);

            emailService.sendEmail(
                appProperties.notifications().recipients().split(","),
                appProperties.notifications().from(),
                appProperties.notifications().senderName(),
                "🧪 Test Email - Stock Signal Tracker [" + timestamp + "]",
                htmlBody
            );

            log.info("✅ Test email sent successfully!");
            return Map.of(
                "ok", true,
                "message", "Test email sent successfully!",
                "provider", appProperties.email().provider(),
                "to", appProperties.notifications().recipients(),
                "from", appProperties.notifications().from(),
                "timestamp", timestamp,
                "info", "Check console for 'Email saved to' message (local mode) or inbox (SMTP mode)"
            );
        } catch (Exception exception) {
            log.error("❌ Test email failed: {}", exception.getMessage(), exception);
            return Map.of(
                "ok", false,
                "message", "Test email failed",
                "error", exception.getMessage(),
                "provider", appProperties.email().provider(),
                "from", appProperties.notifications().from(),
                "to", appProperties.notifications().recipients()
            );
        }
    }

    @PostMapping("/email/intraday")
    public Map<String, Object> testIntradayEmail() {
        try {
            log.info("🧪 Testing intraday email...");
            intradayEmailSchedulerService.runIntradayEmail();
            return Map.of(
                "ok", true,
                "message", "Intraday email test triggered successfully",
                "info", "Check console logs and ./data/emails/ directory for the generated email"
            );
        } catch (Exception exception) {
            log.error("❌ Email test failed: {}", exception.getMessage(), exception);
            return Map.of(
                "ok", false,
                "message", "Email test failed",
                "error", exception.getMessage()
            );
        }
    }

    private String buildTestEmailBody(String timestamp) {
        return "<html><head><style>"
            + "body { font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px; }"
            + ".container { background: white; max-width: 600px; margin: 0 auto; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
            + ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }"
            + ".header h1 { margin: 0; font-size: 28px; }"
            + ".content { padding: 30px; }"
            + ".info-box { background: #f0f4f8; border-left: 4px solid #667eea; padding: 15px; margin: 15px 0; border-radius: 4px; }"
            + ".success { color: #10b981; font-weight: bold; }"
            + ".timestamp { color: #666; font-size: 12px; }"
            + ".footer { background: #f9f9f9; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e5e5; }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='header'>"
            + "<h1>Test Email</h1>"
            + "<p>Stock Signal Tracker</p>"
            + "</div>"
            + "<div class='content'>"
            + "<h2>Email Configuration is Working!</h2>"
            + "<p>This is a test email to verify that the email service is properly configured and working.</p>"
            + "<div class='info-box'>"
            + "<strong>Test Details:</strong><br>"
            + "Timestamp: " + timestamp + "<br>"
            + "Status: SUCCESS<br>"
            + "Email Service: Working properly"
            + "</div>"
            + "<h3>Next Steps:</h3>"
            + "<ol>"
            + "<li>If you received this email, the configuration is correct!</li>"
            + "<li>The intraday scheduler will automatically send emails every 30 minutes (9 AM - 4 PM IST)</li>"
            + "<li>Emails contain stock alerts organized by watchlist group (V40, V40 NEXT, V200, BANK, NBFC)</li>"
            + "<li>Each alert shows: Entry Price, Current Price, Exit Price, and Potential Gains</li>"
            + "</ol>"
            + "<h3>Email Features:</h3>"
            + "<ul>"
            + "<li>Beautiful gradient header with color-coded signals</li>"
            + "<li>Professional tables with all relevant stock data</li>"
            + "<li>Responsive design for mobile and desktop</li>"
            + "<li>Real-time scanner results</li>"
            + "<li>SMA and V20 strategy support</li>"
            + "</ul>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>Stock Signal Tracker - Intraday Alert System</p>"
            + "<p class='timestamp'>Generated on " + timestamp + "</p>"
            + "</div>"
            + "</div>"
            + "</body></html>";
    }
}
