package com.suhas.stocktracker.service;

import com.suhas.stocktracker.config.AppProperties;
import com.suhas.stocktracker.model.DashboardResponse;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.model.WatchlistRow;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IntradayEmailSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(IntradayEmailSchedulerService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm z", Locale.ENGLISH);

    private final ScannerService scannerService;
    private final DashboardService dashboardService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public IntradayEmailSchedulerService(
        ScannerService scannerService,
        DashboardService dashboardService,
        EmailService emailService,
        AppProperties appProperties
    ) {
        this.scannerService = scannerService;
        this.dashboardService = dashboardService;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "0 */30 9-15 ? * MON-FRI", zone = "Asia/Kolkata")
    public void runIntradayEmail() {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            int minute = now.getMinute();
            int hour = now.getHour();

            // Only run at specific times: 9:00, 9:30, 10:00, 10:30, ..., 15:30
            if ((minute != 0 && minute != 30) || hour < 9 || hour > 15) {
                return;
            }
            log.info("Starting intraday email scheduler at {}", now.format(TIMESTAMP_FORMAT));
            executeIntradayEmail();
        } catch (Exception exception) {
            log.error("Error in intraday email scheduler: ", exception);
        }
    }

    private void executeIntradayEmail() {
        List<String> recipients = notificationRecipients();
        if (recipients.isEmpty()) {
            log.warn("No notification recipients configured. Skipping intraday email.");
            return;
        }

        if (blank(appProperties.notifications().from())) {
            log.warn("Notification sender address is not configured. Skipping intraday email.");
            return;
        }

        try {
            // Scan SMA and V20 strategies only
            List<DashboardResponse> dashboards = new ArrayList<>();
            scannerService.runScanner(StrategyType.SMA);
            dashboards.add(dashboardService.fetchDashboard(StrategyType.SMA));
            scannerService.runScanner(StrategyType.V20);
            dashboards.add(dashboardService.fetchDashboard(StrategyType.V20));

            // Organize by watchlist group
            Map<String, List<WatchlistRow>> dataByGroup = organizeByGroup(dashboards);

            if (dataByGroup.isEmpty() || dataByGroup.values().stream().allMatch(List::isEmpty)) {
                log.info("No active alerts found for intraday email. Skipping.");
                return;
            }

            sendIntradayEmail(recipients, dashboards, dataByGroup);
            log.info("Sent intraday email to {} recipient(s)", recipients.size());
        } catch (Exception exception) {
            log.error("Failed to execute intraday email: ", exception);
        }
    }

    private Map<String, List<WatchlistRow>> organizeByGroup(List<DashboardResponse> dashboards) {
        Map<String, List<WatchlistRow>> grouped = new LinkedHashMap<>();
        grouped.put("V40", new ArrayList<>());
        grouped.put("V40 NEXT", new ArrayList<>());
        grouped.put("V200", new ArrayList<>());
        grouped.put("BANK", new ArrayList<>());
        grouped.put("NBFC", new ArrayList<>());

        for (DashboardResponse dashboard : dashboards) {
            for (WatchlistRow row : dashboard.watchlist()) {
                if (isAlert(row) && grouped.containsKey(row.group())) {
                    grouped.get(row.group()).add(row);
                }
            }
        }

        return grouped;
    }

    private void sendIntradayEmail(
        List<String> recipients,
        List<DashboardResponse> dashboards,
        Map<String, List<WatchlistRow>> dataByGroup
    ) {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            int totalAlerts = dataByGroup.values().stream().mapToInt(List::size).sum();

            emailService.sendEmail(
                recipients.toArray(String[]::new),
                appProperties.notifications().from(),
                appProperties.notifications().senderName(),
                buildEmailSubject(now, totalAlerts),
                buildHtmlBody(now, dashboards, dataByGroup, totalAlerts)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to send intraday email: " + exception.getMessage(), exception);
        }
    }

    private String buildEmailSubject(ZonedDateTime now, int totalAlerts) {
        return String.format(
            "📈 Stock Alerts | %s IST | %d Active Signals",
            now.format(TIME_FORMAT),
            totalAlerts
        );
    }

    private String buildHtmlBody(
        ZonedDateTime now,
        List<DashboardResponse> dashboards,
        Map<String, List<WatchlistRow>> dataByGroup,
        int totalAlerts
    ) {
        StringBuilder html = new StringBuilder();

        // Header
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<style>\n");
        html.append(getCssStyles());
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Main container
        html.append("<div class='container'>\n");

        // Header section
        html.append("<div class='header'>\n");
        html.append("<h1>📈 Stock Signal Alerts</h1>\n");
        html.append("<p class='timestamp'>").append(now.format(TIMESTAMP_FORMAT)).append(" IST</p>\n");
        html.append("<p class='summary'>").append(totalAlerts).append(" Active Signal").append(totalAlerts > 1 ? "s" : "").append("</p>\n");
        html.append("</div>\n");

        // Data by group
        for (Map.Entry<String, List<WatchlistRow>> entry : dataByGroup.entrySet()) {
            String group = entry.getKey();
            List<WatchlistRow> rows = entry.getValue();

            if (!rows.isEmpty()) {
                html.append("<div class='section'>\n");
                html.append("<h2 class='group-title'>").append(group).append("</h2>\n");
                html.append("<div class='table-wrapper'>\n");
                html.append("<table class='data-table'>\n");
                html.append("<thead>\n");
                html.append("<tr>\n");
                html.append("<th>Symbol</th>\n");
                html.append("<th>Strategy</th>\n");
                html.append("<th>Signal</th>\n");
                html.append("<th>Entry Price</th>\n");
                html.append("<th>Current Price</th>\n");
                html.append("<th>Exit Price</th>\n");
                html.append("<th>Potential Gain</th>\n");
                html.append("</tr>\n");
                html.append("</thead>\n");
                html.append("<tbody>\n");

                for (WatchlistRow row : rows) {
                    html.append(buildTableRow(row));
                }

                html.append("</tbody>\n");
                html.append("</table>\n");
                html.append("</div>\n");
                html.append("</div>\n");
            }
        }

        // Footer
        html.append("<div class='footer'>\n");
        html.append("<p>Stock Signal Tracker - Intraday Alert System</p>\n");
        html.append("<p>").append("Generated on ").append(now.format(TIMESTAMP_FORMAT)).append("</p>\n");
        html.append("</div>\n");

        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String buildTableRow(WatchlistRow row) {
        Double entryPrice = row.entryPrice();
        Double currentPrice = row.scannerPrice();
        Double exitPrice = row.targetPrice();
        String signal = row.scannerSignal();

        Double potentialGain = calculatePotentialGain(currentPrice, exitPrice);
        String gainClass = potentialGain != null && potentialGain > 0 ? "positive" : "negative";

        StringBuilder tr = new StringBuilder();
        tr.append("<tr>\n");
        tr.append("<td class='symbol'>").append(escapeHtml(row.symbol())).append("</td>\n");
        tr.append("<td>").append(escapeHtml(row.scannerStrategy() != null ? row.scannerStrategy().toUpperCase() : "-")).append("</td>\n");
        tr.append("<td class='signal signal-").append(signal != null ? signal.toLowerCase() : "none").append("'>\n");
        tr.append("<span class='badge'>").append(signal != null ? signal.toUpperCase() : "-").append("</span>\n");
        tr.append("</td>\n");
        tr.append("<td class='price'>").append(formatPrice(entryPrice)).append("</td>\n");
        tr.append("<td class='price'>").append(formatPrice(currentPrice)).append("</td>\n");
        tr.append("<td class='price'>").append(formatPrice(exitPrice)).append("</td>\n");
        tr.append("<td class='gain ").append(gainClass).append("'>\n");
        tr.append(formatGain(potentialGain)).append("\n");
        tr.append("</td>\n");
        tr.append("</tr>\n");

        return tr.toString();
    }

    private Double calculatePotentialGain(Double currentPrice, Double targetPrice) {
        if (currentPrice == null || currentPrice == 0 || targetPrice == null) {
            return null;
        }
        return ((targetPrice - currentPrice) / currentPrice) * 100;
    }

    private String formatPrice(Double price) {
        if (price == null) {
            return "-";
        }
        return String.format(Locale.ENGLISH, "Rs %.2f", price);
    }

    private String formatGain(Double gain) {
        if (gain == null) {
            return "-";
        }
        String formatted = String.format(Locale.ENGLISH, "%.2f%%", gain);
        if (gain > 0) {
            return "▲ " + formatted;
        } else if (gain < 0) {
            return "▼ " + formatted;
        }
        return formatted;
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

    private String getCssStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                padding: 20px;
                color: #333;
            }

            .container {
                max-width: 1000px;
                margin: 0 auto;
                background: white;
                border-radius: 12px;
                box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                overflow: hidden;
            }

            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 40px 30px;
                text-align: center;
            }

            .header h1 {
                font-size: 28px;
                margin-bottom: 10px;
                font-weight: 600;
            }

            .header .timestamp {
                font-size: 14px;
                opacity: 0.9;
                margin-bottom: 15px;
            }

            .header .summary {
                font-size: 18px;
                font-weight: 500;
                background: rgba(255, 255, 255, 0.2);
                padding: 8px 16px;
                border-radius: 20px;
                display: inline-block;
            }

            .section {
                padding: 30px;
                border-bottom: 1px solid #e5e5e5;
            }

            .section:last-child {
                border-bottom: none;
            }

            .group-title {
                font-size: 18px;
                color: #667eea;
                margin-bottom: 20px;
                font-weight: 600;
                text-transform: uppercase;
                letter-spacing: 1px;
            }

            .table-wrapper {
                overflow-x: auto;
            }

            .data-table {
                width: 100%;
                border-collapse: collapse;
                font-size: 13px;
            }

            .data-table thead {
                background: #f8f9fa;
                border-bottom: 2px solid #667eea;
            }

            .data-table th {
                padding: 12px 8px;
                text-align: left;
                font-weight: 600;
                color: #333;
                white-space: nowrap;
            }

            .data-table td {
                padding: 12px 8px;
                border-bottom: 1px solid #e5e5e5;
                vertical-align: middle;
            }

            .data-table tbody tr:hover {
                background: #f9f9f9;
            }

            .data-table tbody tr:last-child td {
                border-bottom: none;
            }

            .symbol {
                font-weight: 600;
                color: #667eea;
            }

            .price {
                text-align: right;
                font-weight: 500;
            }

            .signal {
                text-align: center;
            }

            .badge {
                display: inline-block;
                padding: 4px 12px;
                border-radius: 20px;
                font-size: 12px;
                font-weight: 600;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            .signal-buy .badge {
                background: #10b981;
                color: white;
            }

            .signal-sell .badge {
                background: #ef4444;
                color: white;
            }

            .signal-alert .badge {
                background: #f59e0b;
                color: white;
            }

            .gain {
                text-align: right;
                font-weight: 600;
            }

            .gain.positive {
                color: #10b981;
            }

            .gain.negative {
                color: #ef4444;
            }

            .footer {
                background: #f8f9fa;
                padding: 20px 30px;
                text-align: center;
                border-top: 1px solid #e5e5e5;
                color: #666;
                font-size: 12px;
            }

            .footer p {
                margin: 5px 0;
            }

            @media (max-width: 600px) {
                .header {
                    padding: 20px;
                }

                .header h1 {
                    font-size: 20px;
                }

                .section {
                    padding: 15px;
                }

                .data-table {
                    font-size: 12px;
                }

                .data-table th,
                .data-table td {
                    padding: 8px 4px;
                }
            }
            """;
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
