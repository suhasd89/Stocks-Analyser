package com.suhas.stocktracker.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Scanner scanner, Watchlists watchlists, Scheduler scheduler, Notifications notifications) {

    public record Scanner(String range, String interval, String strategyName, long pauseMillis) {
    }

    public record Watchlists(Map<String, String> resources) {
    }

    public record Scheduler(String timezone, Job hourly, Job daily) {
    }

    public record Job(boolean enabled, String cron) {
    }

    public record Notifications(String recipients, String from, String senderName, boolean hourlyIncludeEmpty,
                                boolean dailyIncludeEmpty) {
    }
}
