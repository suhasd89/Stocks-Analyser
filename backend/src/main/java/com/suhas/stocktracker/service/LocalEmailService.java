package com.suhas.stocktracker.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "local")
public class LocalEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(LocalEmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String EMAIL_DIR = "./data/emails";

    public LocalEmailService() {
        // Create emails directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(EMAIL_DIR));
            log.info("Local email directory: {}", new File(EMAIL_DIR).getAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to create email directory: {}", e.getMessage());
        }
    }

    @Override
    public void sendEmail(String[] recipients, String from, String senderName, String subject, String htmlBody) {
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String fileName = String.format("email_%s.html", timestamp.replace(":", "-").replace(" ", "_"));
            String filePath = Paths.get(EMAIL_DIR, fileName).toString();

            // Create email file with metadata
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<!DOCTYPE html>\n");
            emailContent.append("<html>\n");
            emailContent.append("<head>\n");
            emailContent.append("<meta charset='UTF-8'>\n");
            emailContent.append("<style>\n");
            emailContent.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            emailContent.append(".metadata { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; border: 1px solid #ddd; }\n");
            emailContent.append(".metadata-row { margin: 8px 0; }\n");
            emailContent.append(".label { font-weight: bold; color: #333; }\n");
            emailContent.append(".value { color: #666; margin-left: 10px; }\n");
            emailContent.append(".recipients { background: #e8f4f8; padding: 10px; margin: 5px 0; border-radius: 3px; }\n");
            emailContent.append(".divider { border-top: 2px solid #ddd; margin: 20px 0; }\n");
            emailContent.append(".email-content { background: white; }\n");
            emailContent.append("</style>\n");
            emailContent.append("</head>\n");
            emailContent.append("<body>\n");

            // Metadata section
            emailContent.append("<div class='metadata'>\n");
            emailContent.append("<h2 style='color: #667eea; margin-top: 0;'>📧 Local Email (POC)</h2>\n");
            emailContent.append("<div class='metadata-row'>\n");
            emailContent.append("<span class='label'>From:</span>\n");
            emailContent.append("<span class='value'>").append(escapeHtml(senderName)).append(" &lt;").append(escapeHtml(from)).append("&gt;</span>\n");
            emailContent.append("</div>\n");
            emailContent.append("<div class='metadata-row'>\n");
            emailContent.append("<span class='label'>To:</span>\n");
            emailContent.append("<div class='recipients'>\n");
            for (String recipient : recipients) {
                emailContent.append("&nbsp;&nbsp;").append(escapeHtml(recipient)).append("<br>\n");
            }
            emailContent.append("</div>\n");
            emailContent.append("</div>\n");
            emailContent.append("<div class='metadata-row'>\n");
            emailContent.append("<span class='label'>Subject:</span>\n");
            emailContent.append("<span class='value'>").append(escapeHtml(subject)).append("</span>\n");
            emailContent.append("</div>\n");
            emailContent.append("<div class='metadata-row'>\n");
            emailContent.append("<span class='label'>Timestamp:</span>\n");
            emailContent.append("<span class='value'>").append(timestamp).append("</span>\n");
            emailContent.append("</div>\n");
            emailContent.append("<div class='metadata-row'>\n");
            emailContent.append("<span class='label'>File:</span>\n");
            emailContent.append("<span class='value'>").append(fileName).append("</span>\n");
            emailContent.append("</div>\n");
            emailContent.append("</div>\n");

            // Email content
            emailContent.append("<div class='divider'></div>\n");
            emailContent.append("<div class='email-content'>\n");
            emailContent.append(htmlBody);
            emailContent.append("</div>\n");

            emailContent.append("</body>\n");
            emailContent.append("</html>\n");

            // Write to file
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(emailContent.toString());
            }

            // Log to console
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📧 LOCAL EMAIL POC - Email saved successfully");
            log.info("───────────────────────────────────────────────────────────────");
            log.info("From: {} <{}>", senderName, from);
            log.info("To: {}", Arrays.toString(recipients));
            log.info("Subject: {}", subject);
            log.info("File: {}", filePath);
            log.info("Timestamp: {}", timestamp);
            log.info("───────────────────────────────────────────────────────────────");
            log.info("✓ Email saved to: {}", new File(filePath).getAbsolutePath());
            log.info("✓ Open this file in a browser to preview the email");
            log.info("═══════════════════════════════════════════════════════════════");
        } catch (IOException exception) {
            log.error("Failed to save local email: {}", exception.getMessage(), exception);
            throw new IllegalStateException("Failed to save local email: " + exception.getMessage(), exception);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
