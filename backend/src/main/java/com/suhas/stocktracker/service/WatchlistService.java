package com.suhas.stocktracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.suhas.stocktracker.config.AppProperties;
import com.suhas.stocktracker.model.WatchlistAdminResponse;
import com.suhas.stocktracker.model.WatchlistGroupSummary;
import com.suhas.stocktracker.model.WatchlistReplaceResponse;
import com.suhas.stocktracker.model.WatchlistUpsertRequest;
import com.suhas.stocktracker.model.WatchlistUpsertResponse;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.model.WatchlistStock;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {
    private final ObjectMapper yamlMapper;
    private final AppProperties appProperties;
    private final DatabaseService databaseService;
    private final List<WatchlistStock> seedWatchlist;

    public WatchlistService(AppProperties appProperties, DatabaseService databaseService) throws IOException {
        this.yamlMapper = new YAMLMapper();
        this.appProperties = appProperties;
        this.databaseService = databaseService;
        this.seedWatchlist = loadSeedWatchlist();
    }

    @PostConstruct
    void seedDatabaseIfNeeded() {
        if (databaseService.hasWatchlistStocks()) {
            return;
        }
        Map<String, List<WatchlistStock>> byGroup = new LinkedHashMap<>();
        for (WatchlistStock stock : seedWatchlist) {
            byGroup.computeIfAbsent(stock.group(), ignored -> new ArrayList<>()).add(stock);
        }
        byGroup.forEach(databaseService::replaceWatchlistGroup);
    }

    public List<WatchlistStock> getWatchlist() {
        return databaseService.fetchWatchlistStocks();
    }

    public List<WatchlistStock> getWatchlistForStrategy(StrategyType strategyType) {
        return getWatchlist().stream()
            .filter(stock -> isEligibleForStrategy(stock, strategyType))
            .toList();
    }

    private boolean isEligibleForStrategy(WatchlistStock stock, StrategyType strategyType) {
        if (strategyType == StrategyType.SMA) {
            return "V40".equalsIgnoreCase(stock.group());
        }
        return true;
    }

    public WatchlistAdminResponse fetchAdminWatchlists() {
        return new WatchlistAdminResponse(databaseService.fetchWatchlistGroupSummaries(), getWatchlist());
    }

    public WatchlistReplaceResponse replaceGroup(String group, String rawText) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("group is required");
        }
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("rawText is required");
        }

        String normalizedGroup = normalizeGroup(group);
        List<String> lines = rawText.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .map(line -> line.replaceFirst("^[-*]\\s*", ""))
            .toList();

        List<WatchlistStock> catalog = buildCatalog();
        Map<String, WatchlistStock> byNormalizedName = new LinkedHashMap<>();
        Map<String, WatchlistStock> byNormalizedSymbol = new LinkedHashMap<>();
        for (WatchlistStock stock : catalog) {
            byNormalizedName.putIfAbsent(normalizeValue(stock.name()), stock);
            byNormalizedSymbol.putIfAbsent(normalizeValue(stock.symbol()), stock);
        }

        List<WatchlistStock> resolved = new ArrayList<>();
        List<String> guessed = new ArrayList<>();
        LinkedHashSet<String> seenSymbols = new LinkedHashSet<>();
        for (String rawLine : lines) {
            WatchlistStock stock = byNormalizedName.get(normalizeValue(rawLine));
            if (stock == null) {
                stock = byNormalizedSymbol.get(normalizeValue(rawLine));
            }
            if (stock == null) {
                stock = guessedStock(rawLine, normalizedGroup);
                guessed.add(rawLine + " -> " + stock.symbol());
            } else {
                stock = new WatchlistStock(stock.symbol(), stock.name(), normalizedGroup, stock.yahooSymbol());
            }
            if (seenSymbols.add(stock.symbol())) {
                resolved.add(stock);
            }
        }

        databaseService.replaceWatchlistGroup(normalizedGroup, resolved);
        return new WatchlistReplaceResponse(
            true,
            normalizedGroup,
            resolved.size(),
            guessed,
            "Stored " + resolved.size() + " stocks for " + normalizedGroup + "."
        );
    }

    public WatchlistUpsertResponse upsertStock(WatchlistUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.group() == null || request.group().isBlank()) {
            throw new IllegalArgumentException("group is required");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.yahooSymbol() == null || request.yahooSymbol().isBlank()) {
            throw new IllegalArgumentException("yahooSymbol is required");
        }

        String normalizedGroup = normalizeGroup(request.group());
        String symbol = normalizeTicker(request.symbol());
        String yahooSymbol = request.yahooSymbol().trim().toUpperCase(Locale.ROOT);
        WatchlistStock stock = new WatchlistStock(
            symbol,
            request.name().trim(),
            normalizedGroup,
            yahooSymbol
        );
        databaseService.upsertWatchlistStock(stock);
        return new WatchlistUpsertResponse(
            true,
            normalizedGroup,
            symbol,
            yahooSymbol,
            "Saved " + symbol + " in " + normalizedGroup + "."
        );
    }

    private List<WatchlistStock> loadSeedWatchlist() throws IOException {
        List<WatchlistStock> loaded = new ArrayList<>();
        for (Map.Entry<String, String> entry : appProperties.watchlists().resources().entrySet()) {
            loaded.addAll(readClasspathWatchlist(entry.getValue(), entry.getKey()));
        }
        return loaded;
    }

    private List<WatchlistStock> readClasspathWatchlist(String resourceName, String group) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourceName).getInputStream()) {
            return readWatchlist(inputStream, group);
        }
    }

    private List<WatchlistStock> readWatchlist(InputStream inputStream, String group) throws IOException {
        WatchlistFile watchlistFile = yamlMapper.readValue(inputStream, WatchlistFile.class);
        return watchlistFile.stocks().stream()
            .map(stock -> new WatchlistStock(stock.symbol(), stock.name(), group, stock.yahooSymbol()))
            .toList();
    }

    private List<WatchlistStock> buildCatalog() {
        List<WatchlistStock> catalog = new ArrayList<>(seedWatchlist);
        catalog.addAll(getWatchlist());
        return catalog;
    }

    private WatchlistStock guessedStock(String rawLine, String group) {
        String symbol = normalizeTicker(rawLine);
        if (symbol.isBlank()) {
            symbol = "UNKNOWN";
        }
        return new WatchlistStock(symbol, rawLine.trim(), group, symbol + ".NS");
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String normalizeTicker(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
    }

    private String normalizeGroup(String group) {
        String normalized = group.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if ("V40NEXT".equals(normalized.replace(" ", ""))) {
            return "V40 NEXT";
        }
        return normalized;
    }

    private record WatchlistFile(List<WatchlistEntry> stocks) {
    }

    private record WatchlistEntry(String symbol, String name, String yahooSymbol) {
    }
}
