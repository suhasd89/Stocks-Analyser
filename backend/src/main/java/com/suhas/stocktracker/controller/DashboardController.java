package com.suhas.stocktracker.controller;

import com.suhas.stocktracker.model.DashboardResponse;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.service.DashboardService;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "serverTime", OffsetDateTime.now().toString());
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(defaultValue = "sma") String strategy) {
        DashboardResponse response = dashboardService.fetchDashboard(StrategyType.fromSlug(strategy));
        log.debug("Dashboard fetched. strategy={}, watchlist={}, activeSignals={}, failures={}",
            response.strategy(),
            response.watchlist().size(),
            response.summary().getOrDefault("activeSignals", 0),
            response.scanner().failures().size());
        return response;
    }
}
