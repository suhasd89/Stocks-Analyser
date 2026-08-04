package com.suhas.stocktracker.service;

import com.suhas.stocktracker.config.AppProperties;
import com.suhas.stocktracker.model.WatchlistStock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MarketUniverseService {
    public static final String NSE_EQ_GROUP = "NSE EQ";
    private static final Logger log = LoggerFactory.getLogger(MarketUniverseService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final RestClient restClient;
    private final AppProperties appProperties;
    private volatile CachedUniverse cachedUniverse;

    public MarketUniverseService(RestClient restClient, AppProperties appProperties) {
        this.restClient = restClient;
        this.appProperties = appProperties;
    }

    public List<WatchlistStock> fetchNseEquityUniverse() {
        CachedUniverse current = cachedUniverse;
        if (current != null && Duration.between(current.fetchedAt(), Instant.now()).compareTo(CACHE_TTL) < 0) {
            return current.stocks();
        }

        String rawCsv = restClient.get()
            .uri(appProperties.marketUniverse().nseEquityListUrl())
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE + ",text/csv,*/*")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
            .retrieve()
            .body(String.class);

        List<WatchlistStock> stocks = parseNseEquityCsv(rawCsv);
        int maxSymbols = appProperties.marketUniverse().maxSymbols();
        if (maxSymbols > 0 && stocks.size() > maxSymbols) {
            stocks = stocks.subList(0, maxSymbols);
            log.warn("NSE EQ universe limited to {} symbols by APP_MARKET_UNIVERSE_MAX_SYMBOLS.", maxSymbols);
        }
        cachedUniverse = new CachedUniverse(Instant.now(), List.copyOf(stocks));
        log.info("Loaded {} NSE EQ symbols for multibagger discovery.", stocks.size());
        return cachedUniverse.stocks();
    }

    private List<WatchlistStock> parseNseEquityCsv(String rawCsv) {
        if (rawCsv == null || rawCsv.isBlank()) {
            throw new IllegalStateException("NSE equity universe CSV is empty");
        }

        List<String> lines = rawCsv.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
        if (lines.size() < 2) {
            throw new IllegalStateException("NSE equity universe CSV has no rows");
        }

        List<String> headers = parseCsvLine(lines.getFirst());
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            headerIndex.put(normalizeHeader(headers.get(index)), index);
        }

        int symbolIndex = requireColumn(headerIndex, "SYMBOL");
        int nameIndex = requireColumn(headerIndex, "NAMEOFCOMPANY");
        int seriesIndex = requireColumn(headerIndex, "SERIES");

        List<WatchlistStock> stocks = new ArrayList<>();
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            List<String> cells = parseCsvLine(lines.get(lineIndex));
            if (cells.size() <= Math.max(symbolIndex, Math.max(nameIndex, seriesIndex))) {
                continue;
            }
            String series = cells.get(seriesIndex).trim().toUpperCase(Locale.ROOT);
            if (!"EQ".equals(series)) {
                continue;
            }
            String symbol = cells.get(symbolIndex).trim().toUpperCase(Locale.ROOT);
            String name = cells.get(nameIndex).trim();
            if (symbol.isBlank() || name.isBlank()) {
                continue;
            }
            stocks.add(new WatchlistStock(symbol, name, NSE_EQ_GROUP, symbol + ".NS"));
        }
        if (stocks.isEmpty()) {
            throw new IllegalStateException("NSE equity universe CSV did not contain EQ symbols");
        }
        return stocks;
    }

    private int requireColumn(Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key);
        if (index == null) {
            throw new IllegalStateException("NSE equity universe CSV missing column " + key);
        }
        return index;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == ',' && !quoted) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private record CachedUniverse(Instant fetchedAt, List<WatchlistStock> stocks) {
    }
}
