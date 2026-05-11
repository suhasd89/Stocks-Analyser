package com.suhas.stocktracker.model;

public record WatchlistUpsertRequest(
    String group,
    String symbol,
    String name,
    String yahooSymbol
) {
}
