package com.suhas.stocktracker.controller;

import com.suhas.stocktracker.model.ScannerRunResponse;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.service.ScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scanner")
public class ScannerController {
    private static final Logger log = LoggerFactory.getLogger(ScannerController.class);

    private final ScannerService scannerService;

    public ScannerController(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    @PostMapping("/run")
    public ScannerRunResponse run(@RequestParam(defaultValue = "sma") String strategy,
                                  @RequestParam(required = false) String group) {
        log.info("Scanner run requested. strategy={}, group={}", strategy, group == null ? "ALL" : group);
        ScannerRunResponse response = scannerService.runScanner(StrategyType.fromSlug(strategy), group);
        log.info("Scanner run completed. strategy={}, group={}, runId={}, scanned={}, failed={}",
            response.strategy(), group == null ? "ALL" : group, response.runId(), response.scanned(), response.failed().size());
        return response;
    }
}
