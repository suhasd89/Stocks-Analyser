package com.suhas.stocktracker.model;

public record WatchlistUpsertResponse(
    boolean ok,
    String group,
    String symbol,
    String yahooSymbol,
    String message
) {
}
