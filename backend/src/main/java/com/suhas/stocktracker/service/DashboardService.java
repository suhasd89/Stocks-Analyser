package com.suhas.stocktracker.service;

import com.suhas.stocktracker.model.DashboardResponse;
import com.suhas.stocktracker.model.ScannerFailure;
import com.suhas.stocktracker.model.ScannerResult;
import com.suhas.stocktracker.model.ScannerRun;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.model.WatchlistRow;
import com.suhas.stocktracker.model.WatchlistStock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final WatchlistService watchlistService;
    private final MarketUniverseService marketUniverseService;
    private final DatabaseService databaseService;

    public DashboardService(WatchlistService watchlistService, MarketUniverseService marketUniverseService,
                            DatabaseService databaseService) {
        this.watchlistService = watchlistService;
        this.marketUniverseService = marketUniverseService;
        this.databaseService = databaseService;
    }

    public DashboardResponse fetchDashboard(StrategyType strategyType) {
        List<ScannerResult> scanResults = databaseService.fetchScannerResults(strategyType);
        Map<String, ScannerResult> scansByTicker = scanResults.stream()
            .collect(Collectors.toMap(ScannerResult::ticker, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        ScannerRun latestRun = databaseService.fetchLatestScannerRun(strategyType);
        List<ScannerFailure> failures = latestRun == null
            ? List.of()
            : databaseService.fetchScannerFailuresForRun(latestRun.id());

        List<WatchlistRow> rows = strategyType == StrategyType.MULTIBAGGER
            ? buildMultibaggerRows(scanResults)
            : buildWatchlistRows(strategyType, scansByTicker);

        int buySignals = (int) rows.stream().filter(row -> "BUY".equals(row.scannerSignal()) || "ALERT".equals(row.scannerSignal())).count();
        int sellSignals = strategyType == StrategyType.MULTIBAGGER
            ? (int) rows.stream().filter(row -> "WATCH".equals(row.scannerSignal())).count()
            : (int) rows.stream().filter(row -> "SELL".equals(row.scannerSignal())).count();
        int trackedStocks = strategyType == StrategyType.MULTIBAGGER
            ? latestRun == null ? scansByTicker.size() : latestRun.stocksScanned()
            : watchlistService.getWatchlistForStrategy(strategyType).size();

        return new DashboardResponse(
            strategyType.slug(),
            rows,
            Map.of(
                "trackedStocks", trackedStocks,
                "activeSignals", buySignals + sellSignals,
                "buySignals", buySignals,
                "sellSignals", sellSignals,
                "failedStocks", failures.size()
            ),
            new DashboardResponse.ScannerSummary(latestRun, scansByTicker.size(), failures),
            OffsetDateTime.now().toString()
        );
    }

    private List<WatchlistRow> buildWatchlistRows(StrategyType strategyType, Map<String, ScannerResult> scansByTicker) {
        return watchlistService.getWatchlistForStrategy(strategyType).stream()
            .map(stock -> toRow(strategyType, stock, scansByTicker.get(stock.symbol())))
            .toList();
    }

    private List<WatchlistRow> buildMultibaggerRows(List<ScannerResult> scanResults) {
        if (scanResults.isEmpty()) {
            return List.of();
        }
        Map<String, WatchlistStock> universeBySymbol;
        try {
            universeBySymbol = marketUniverseService.fetchNseEquityUniverse().stream()
                .collect(Collectors.toMap(WatchlistStock::symbol, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        } catch (Exception ignored) {
            universeBySymbol = Map.of();
        }

        Map<String, WatchlistStock> finalUniverseBySymbol = universeBySymbol;
        return scanResults.stream()
            .filter(scan -> "ALERT".equals(scan.signal()) || "WATCH".equals(scan.signal()))
            .sorted(Comparator.comparing(
                ScannerResult::scannerScore,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .map(scan -> {
                WatchlistStock stock = finalUniverseBySymbol.getOrDefault(
                    scan.ticker(),
                    new WatchlistStock(scan.ticker(), scan.ticker(), MarketUniverseService.NSE_EQ_GROUP, scan.yahooSymbol())
                );
                return toRow(StrategyType.MULTIBAGGER, stock, scan);
            })
            .toList();
    }

    private WatchlistRow toRow(StrategyType strategyType, WatchlistStock stock, ScannerResult scan) {
        return new WatchlistRow(
            stock.symbol(),
            stock.name(),
            stock.group(),
            stock.yahooSymbol(),
            strategyType.slug(),
            scan != null ? scan.signal() : "NONE",
            scan != null ? scan.strategy() : strategyType.displayName(),
            scan != null ? scan.lastClose() : null,
            scan != null ? scan.signalDate() : null,
            scan != null ? scan.scannedAt() : null,
            scan != null ? scan.notes() : "",
            scan != null ? scan.sma20() : null,
            scan != null ? scan.sma50() : null,
            scan != null ? scan.sma200() : null,
            scan != null ? scan.percentMove() : null,
            scan != null ? scan.entryPrice() : null,
            scan != null ? scan.targetPrice() : null,
            scan != null ? scan.sequenceStartDate() : null,
            scan != null ? scan.sequenceEndDate() : null,
            scan != null ? scan.percentBelowLifetimeHigh() : null,
            scan != null ? scan.high52Week() : null,
            scan != null ? scan.low52Week() : null,
            scan != null ? scan.scannerScore() : null,
            scan != null ? scan.notes() : ""
        );
    }
}
