package com.suhas.stocktracker.model;

public record ScannerFailure(
    long runId,
    String strategySlug,
    String symbol,
    String name,
    String group,
    String yahooSymbol,
    String error,
    String failedAt
) {
}
