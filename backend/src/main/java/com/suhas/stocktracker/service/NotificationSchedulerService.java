package com.suhas.stocktracker.service;

import com.suhas.stocktracker.config.AppProperties;
import com.suhas.stocktracker.model.DashboardResponse;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.model.WatchlistRow;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulerService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    private final ScannerService scannerService;
    private final DashboardService dashboardService;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public NotificationSchedulerService(ScannerService scannerService, DashboardService dashboardService,
                                        JavaMailSender mailSender, AppProperties appProperties) {
        this.scannerService = scannerService;
        this.dashboardService = dashboardService;
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "${app.scheduler.hourly.cron}", zone = "${app.scheduler.timezone}")
    public void runHourlySummary() {
        if (!appProperties.scheduler().hourly().enabled()) {
            return;
        }
        executeScheduledSummary("hourly", appProperties.notifications().hourlyIncludeEmpty());
    }

    @Scheduled(cron = "${app.scheduler.daily.cron}", zone = "${app.scheduler.timezone}")
    public void runDailySummary() {
        if (!appProperties.scheduler().daily().enabled()) {
            return;
        }
        executeScheduledSummary("daily", appProperties.notifications().dailyIncludeEmpty());
    }

    public String sendManualSummary(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "hourly" -> executeScheduledSummary("hourly", appProperties.notifications().hourlyIncludeEmpty());
            case "daily" -> executeScheduledSummary("daily", appProperties.notifications().dailyIncludeEmpty());
            default -> throw new IllegalArgumentException("Unknown summary mode: " + mode);
        };
    }

    private synchronized String executeScheduledSummary(String mode, boolean includeEmpty) {
        List<DashboardResponse> dashboards = new ArrayList<>();
        for (StrategyType strategyType : StrategyType.values()) {
            scannerService.runScanner(strategyType);
            dashboards.add(dashboardService.fetchDashboard(strategyType));
        }

        int activeAlerts = dashboards.stream()
            .mapToInt(dashboard -> (int) dashboard.watchlist().stream().filter(this::isAlert).count())
            .sum();

        if (activeAlerts == 0 && !includeEmpty) {
            String message = "No active alerts found for " + mode + " summary. Email skipped.";
            log.info(message);
            return message;
        }

        List<String> recipients = notificationRecipients();
        if (recipients.isEmpty()) {
            String message = "No notification recipients configured. Scan completed but email was skipped.";
            log.warn(message);
            return message;
        }
        if (blank(appProperties.notifications().from())) {
            String message = "Notification sender address is not configured. Scan completed but email was skipped.";
            log.warn(message);
            return message;
        }

        sendSummaryEmail(mode, recipients, dashboards, activeAlerts);
        String message = "Sent " + mode + " summary email to " + recipients.size() + " recipient(s) with "
            + activeAlerts + " active alert(s).";
        log.info(message);
        return message;
    }

    private void sendSummaryEmail(String mode, List<String> recipients, List<DashboardResponse> dashboards, int activeAlerts) {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(appProperties.scheduler().timezone()));
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(recipients.toArray(String[]::new));
            helper.setFrom(new InternetAddress(
                appProperties.notifications().from(),
                appProperties.notifications().senderName(),
                StandardCharsets.UTF_8.name()
            ));
            helper.setSubject(buildSubject(mode, now, activeAlerts));
            helper.setText(buildBody(mode, now, dashboards, activeAlerts), false);
            mailSender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to send summary email: " + exception.getMessage(), exception);
        }
    }

    private String buildSubject(String mode, ZonedDateTime now, int activeAlerts) {
        return "Stock Signal " + capitalize(mode) + " Summary | "
            + now.format(TIMESTAMP_FORMAT) + " | " + activeAlerts + " active alerts";
    }

    private String buildBody(String mode, ZonedDateTime now, List<DashboardResponse> dashboards, int activeAlerts) {
        StringBuilder builder = new StringBuilder();
        builder.append("Stock Signal ").append(capitalize(mode)).append(" Summary").append('\n');
        builder.append("Generated: ").append(now.format(TIMESTAMP_FORMAT)).append('\n');
        builder.append("Total active alerts: ").append(activeAlerts).append('\n');
        builder.append('\n');

        for (DashboardResponse dashboard : dashboards) {
            List<WatchlistRow> rows = dashboard.watchlist().stream().filter(this::isAlert).toList();
            builder.append(dashboard.strategy().toUpperCase(Locale.ROOT)).append(" Strategy").append('\n');
            builder.append("Tracked stocks: ").append(dashboard.summary().getOrDefault("trackedStocks", 0)).append('\n');
            builder.append("Active alerts: ").append(rows.size()).append('\n');
            if (dashboard.scanner() != null && dashboard.scanner().lastRun() != null) {
                builder.append("Last scan status: ").append(dashboard.scanner().lastRun().status()).append('\n');
            }
            if (rows.isEmpty()) {
                builder.append("No active alerts.").append('\n');
            } else {
                for (WatchlistRow row : rows) {
                    builder.append("- ")
                        .append(row.symbol())
                        .append(" [").append(row.group()).append("] ")
                        .append(row.scannerSignal());
                    if ("v20".equalsIgnoreCase(dashboard.strategy())) {
                        builder.append(" | Entry ").append(formatCurrency(row.entryPrice()))
                            .append(" | Target ").append(formatCurrency(row.targetPrice()));
                        if (row.sequenceStartDate() != null && row.sequenceEndDate() != null) {
                            builder.append(" | Formation ")
                                .append(formatDate(row.sequenceStartDate()))
                                .append(" - ")
                                .append(formatDate(row.sequenceEndDate()));
                        }
                    } else if ("multibagger".equalsIgnoreCase(dashboard.strategy())) {
                        builder.append(" | Score ")
                            .append(row.scannerScore() == null ? "-" : String.format(Locale.ENGLISH, "%.0f", row.scannerScore()))
                            .append(" | Close ").append(formatCurrency(row.scannerPrice()));
                    } else {
                        builder.append(" | Close ").append(formatCurrency(row.scannerPrice()));
                    }
                    if (row.scannerSignalDate() != null) {
                        builder.append(" | Signal date ").append(formatDate(row.scannerSignalDate()));
                    }
                    builder.append('\n');
                }
            }
            builder.append('\n');
        }

        builder.append("Generated by the Stock Signal Tracker backend running on schedule.");
        return builder.toString();
    }

    private boolean isAlert(WatchlistRow row) {
        return "BUY".equalsIgnoreCase(row.scannerSignal())
            || "SELL".equalsIgnoreCase(row.scannerSignal())
            || "ALERT".equalsIgnoreCase(row.scannerSignal());
    }

    private List<String> notificationRecipients() {
        String raw = appProperties.notifications().recipients();
        if (blank(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .toList();
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ENGLISH, "Rs %.2f", value);
    }

    private String formatDate(String value) {
        try {
            return DATE_FORMAT.format(ZonedDateTime.parse(value));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String capitalize(String value) {
        if (blank(value)) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
