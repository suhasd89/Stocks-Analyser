package com.suhas.stocktracker.controller;

import com.suhas.stocktracker.service.IntradayEmailSchedulerService;
import com.suhas.stocktracker.service.NotificationSchedulerService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationSchedulerService notificationSchedulerService;
    private final IntradayEmailSchedulerService intradayEmailSchedulerService;

    public NotificationController(
        NotificationSchedulerService notificationSchedulerService,
        IntradayEmailSchedulerService intradayEmailSchedulerService
    ) {
        this.notificationSchedulerService = notificationSchedulerService;
        this.intradayEmailSchedulerService = intradayEmailSchedulerService;
    }

    @PostMapping("/send-summary")
    public Map<String, Object> sendSummary(@RequestParam(defaultValue = "daily") String mode) {
        String message = notificationSchedulerService.sendManualSummary(mode);
        return Map.of("ok", true, "mode", mode, "message", message);
    }

    @PostMapping("/send-intraday")
    public Map<String, Object> sendIntradayAlert() {
        intradayEmailSchedulerService.runIntradayEmail();
        return Map.of("ok", true, "message", "Intraday email scheduled successfully");
    }
}
