package com.suhas.stocktracker.controller;

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

    public NotificationController(NotificationSchedulerService notificationSchedulerService) {
        this.notificationSchedulerService = notificationSchedulerService;
    }

    @PostMapping("/send-summary")
    public Map<String, Object> sendSummary(@RequestParam(defaultValue = "daily") String mode) {
        String message = notificationSchedulerService.sendManualSummary(mode);
        return Map.of("ok", true, "mode", mode, "message", message);
    }
}
