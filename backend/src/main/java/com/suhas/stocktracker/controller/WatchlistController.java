package com.suhas.stocktracker.controller;

import com.suhas.stocktracker.model.WatchlistAdminResponse;
import com.suhas.stocktracker.model.WatchlistReplaceRequest;
import com.suhas.stocktracker.model.WatchlistReplaceResponse;
import com.suhas.stocktracker.model.WatchlistUpsertRequest;
import com.suhas.stocktracker.model.WatchlistUpsertResponse;
import com.suhas.stocktracker.service.WatchlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistController {
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public WatchlistAdminResponse watchlists() {
        return watchlistService.fetchAdminWatchlists();
    }

    @PostMapping("/replace")
    public WatchlistReplaceResponse replace(@RequestBody WatchlistReplaceRequest request) {
        log.info("Watchlist replace requested. group={}", request.group());
        WatchlistReplaceResponse response = watchlistService.replaceGroup(request.group(), request.rawText());
        log.info("Watchlist replace completed. group={}, count={}, guessed={}",
            response.group(), response.count(), response.guessedSymbols().size());
        return response;
    }

    @PostMapping("/stock")
    public WatchlistUpsertResponse upsertStock(@RequestBody WatchlistUpsertRequest request) {
        log.info("Single stock upsert requested. group={}, symbol={}, yahooSymbol={}, originalGroup={}, originalSymbol={}",
            request.group(), request.symbol(), request.yahooSymbol(), request.originalGroup(), request.originalSymbol());
        WatchlistUpsertResponse response = watchlistService.upsertStock(request);
        log.info("Single stock upsert completed. group={}, symbol={}, yahooSymbol={}",
            response.group(), response.symbol(), response.yahooSymbol());
        return response;
    }
}
