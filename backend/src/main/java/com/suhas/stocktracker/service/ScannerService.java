package com.suhas.stocktracker.service;

import com.suhas.stocktracker.config.AppProperties;
import com.suhas.stocktracker.model.ScannerFailure;
import com.suhas.stocktracker.model.ScannerResult;
import com.suhas.stocktracker.model.ScannerRunResponse;
import com.suhas.stocktracker.model.StrategyType;
import com.suhas.stocktracker.model.WatchlistStock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
public class ScannerService {
    private static final Logger log = LoggerFactory.getLogger(ScannerService.class);

    private final RestClient restClient;
    private final WatchlistService watchlistService;
    private final MarketUniverseService marketUniverseService;
    private final DatabaseService databaseService;
    private final AppProperties appProperties;

    public ScannerService(RestClient restClient, WatchlistService watchlistService,
                          MarketUniverseService marketUniverseService, DatabaseService databaseService,
                          AppProperties appProperties) {
        this.restClient = restClient;
        this.watchlistService = watchlistService;
        this.marketUniverseService = marketUniverseService;
        this.databaseService = databaseService;
        this.appProperties = appProperties;
    }

    public ScannerRunResponse runScanner(StrategyType strategyType) {
        return runScanner(strategyType, null);
    }

    public ScannerRunResponse runScanner(StrategyType strategyType, String group) {
        Instant started = Instant.now();
        long runId = databaseService.startScannerRun(strategyType);
        List<WatchlistStock> stocks = strategyType == StrategyType.MULTIBAGGER
            ? marketUniverseService.fetchNseEquityUniverse()
            : watchlistService.getWatchlistForStrategy(strategyType, group);
        Map<String, List<WatchlistStock>> stocksByYahooSymbol = new LinkedHashMap<>();
        String scanScope = strategyType == StrategyType.MULTIBAGGER
            ? "NSE EQ universe"
            : group == null || group.isBlank() || "ALL".equalsIgnoreCase(group)
            ? "all eligible lists"
            : group;

        for (WatchlistStock stock : stocks) {
            stocksByYahooSymbol.computeIfAbsent(stock.yahooSymbol(), ignored -> new ArrayList<>()).add(stock);
        }

        int concurrency = Math.max(1, appProperties.scanner().maxConcurrency());
        log.info("Starting {} scanner run {} for {} with {} watchlist rows and {} unique Yahoo symbols using concurrency {}.",
            strategyType.slug(), runId, scanScope, stocks.size(), stocksByYahooSymbol.size(), concurrency);

        List<ScanBatchResult> batches;
        try (ExecutorService executor = Executors.newFixedThreadPool(
            concurrency,
            Thread.ofVirtual().name("scanner-" + strategyType.slug() + "-", 0).factory()
        )) {
            List<CompletableFuture<ScanBatchResult>> futures = stocksByYahooSymbol.entrySet()
                .stream()
                .map(entry -> CompletableFuture.supplyAsync(
                    () -> scanYahooSymbol(runId, strategyType, entry.getKey(), entry.getValue()),
                    executor
                ))
                .toList();

            batches = futures.stream().map(CompletableFuture::join).toList();
        }

        List<ScannerResult> results = batches.stream()
            .map(ScanBatchResult::results)
            .flatMap(Collection::stream)
            .toList();
        List<ScannerFailure> failures = batches.stream()
            .map(ScanBatchResult::failures)
            .flatMap(Collection::stream)
            .toList();

        databaseService.upsertScannerResults(results);
        databaseService.insertScannerFailures(failures);
        String message = failures.isEmpty()
            ? "Scanned " + results.size() + " stocks successfully for " + scanScope + "."
            : "Scanned " + results.size() + " stocks for " + scanScope + ". Failed: " + failures.size() + ".";
        databaseService.finishScannerRun(runId, failures.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS", message, results.size());
        List<String> failed = failures.stream()
            .map(failure -> failure.symbol() + ": " + failure.error())
            .toList();
        long elapsedMillis = Duration.between(started, Instant.now()).toMillis();
        log.info("Finished {} scanner run {} for {}. Results: {}, failures: {}, status: {}, elapsedMs: {}.",
            strategyType.slug(), runId, scanScope, results.size(), failures.size(),
            failures.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS", elapsedMillis);
        return new ScannerRunResponse(true, strategyType.slug(), runId, results.size(), failed, message);
    }

    private ScanBatchResult scanYahooSymbol(long runId, StrategyType strategyType, String yahooSymbol,
                                            List<WatchlistStock> matchingStocks) {
        try {
            List<Candle> candles = fetchCandles(yahooSymbol);
            List<ScannerResult> results = matchingStocks.stream()
                .map(stock -> evaluate(strategyType, stock, candles))
                .toList();
            log.debug("Scanned {} for {} row(s).", yahooSymbol, matchingStocks.size());
            return new ScanBatchResult(results, List.of());
        } catch (Exception exception) {
            String message = exception.getMessage();
            List<ScannerFailure> failures = matchingStocks.stream()
                .map(stock -> scannerFailure(runId, strategyType, stock, message))
                .toList();
            log.warn("Failed to scan Yahoo symbol {} for {} row(s): {}", yahooSymbol, matchingStocks.size(), message);
            return new ScanBatchResult(List.of(), failures);
        }
    }

    private ScannerFailure scannerFailure(long runId, StrategyType strategyType, WatchlistStock stock, String error) {
        return new ScannerFailure(
            runId,
            strategyType.slug(),
            stock.symbol(),
            stock.name(),
            stock.group(),
            stock.yahooSymbol(),
            error == null || error.isBlank() ? "unknown scanner error" : error,
            OffsetDateTime.now().toString()
        );
    }

    private List<Candle> fetchCandles(String yahooSymbol) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?range={range}&interval={interval}&includeAdjustedClose=true";
        JsonNode body = restClient.get()
            .uri(url, yahooSymbol, appProperties.scanner().range(), appProperties.scanner().interval())
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
            .header(HttpHeaders.ORIGIN, "https://finance.yahoo.com")
            .header(HttpHeaders.REFERER, "https://finance.yahoo.com/")
            .retrieve()
            .body(JsonNode.class);

        JsonNode error = body.path("chart").path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String description = error.path("description").asText("Yahoo chart API returned an error");
            throw new IllegalStateException(description);
        }

        JsonNode result = body.path("chart").path("result").get(0);
        if (result == null || result.isMissingNode()) {
            throw new IllegalStateException("missing chart result");
        }

        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").get(0);
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");
        List<Candle> candles = new ArrayList<>();
        for (int index = 0; index < timestamps.size(); index++) {
            JsonNode close = closes.get(index);
            JsonNode open = opens.get(index);
            JsonNode high = highs.get(index);
            JsonNode low = lows.get(index);
            JsonNode volume = volumes.get(index);
            if (close == null || close.isNull() || open == null || open.isNull() || high == null || high.isNull() || low == null || low.isNull()) {
                continue;
            }
            candles.add(new Candle(
                isoFromUnix(timestamps.get(index).asLong()),
                open.asDouble(),
                high.asDouble(),
                low.asDouble(),
                close.asDouble(),
                volume == null || volume.isNull() ? 0 : volume.asLong()
            ));
        }
        if (candles.isEmpty()) {
            throw new IllegalStateException("no candle data returned");
        }
        return candles;
    }

    private ScannerResult evaluate(StrategyType strategyType, WatchlistStock stock, List<Candle> candles) {
        return switch (strategyType) {
            case SMA -> evaluateSma(stock, candles);
            case V20 -> evaluateV20(stock, candles);
            case MULTIBAGGER -> evaluateMultibagger(stock, candles);
        };
    }

    private ScannerResult evaluateSma(WatchlistStock stock, List<Candle> candles) {
        List<Double> closes = new ArrayList<>();
        for (Candle candle : candles) {
            closes.add(candle.close());
        }
        if (closes.size() < 200) {
            throw new IllegalStateException("not enough history to calculate 200 SMA");
        }

        double lastClose = closes.getLast();
        Double sma20 = sma(closes, 20);
        Double sma50 = sma(closes, 50);
        Double sma200 = sma(closes, 200);
        boolean buyRegion = sma200 > sma50 && sma50 > sma20 && sma20 > lastClose;
        boolean sellRegion = lastClose > sma20 && sma20 > sma50 && sma50 > sma200;
        String signal = buyRegion ? "BUY" : sellRegion ? "SELL" : "NONE";
        String signalDate = signal.equals("NONE") ? null : candles.getLast().time();
        String notes = buyRegion
            ? "Price is in buy region based on local daily candle scan."
            : sellRegion
            ? "Price is in sell region based on local daily candle scan."
            : "No active buy/sell region on the latest daily candle.";

        return new ScannerResult(
            StrategyType.SMA.slug(),
            stock.symbol(),
            stock.yahooSymbol(),
            signal,
            StrategyType.SMA.displayName(),
            signalDate,
            lastClose,
            sma20,
            sma50,
            sma200,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            buyRegion,
            sellRegion,
            OffsetDateTime.now().toString(),
            notes
        );
    }

    private ScannerResult evaluateV20(WatchlistStock stock, List<Candle> candles) {
        double minPercentageMove = 20.0;
        Double sequenceEntryLow = null;
        Double sequenceHigh = null;
        boolean sequenceStarted = false;
        Double firstGreenCandleSma200 = null;
        Double latestFormationMove = null;
        Double latestFormationLow = null;
        Double latestFormationHigh = null;
        String latestFormationStartDate = null;
        String latestFormationEndDate = null;
        String currentSequenceStartDate = null;
        int latestFormationEndIndex = -1;
        double lifetimeHigh = Double.NEGATIVE_INFINITY;
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        List<Double> closes = new ArrayList<>();

        for (int candleIndex = 0; candleIndex < candles.size(); candleIndex++) {
            Candle candle = candles.get(candleIndex);
            highs.add(candle.high());
            lows.add(candle.low());
            closes.add(candle.close());

            if (candle.high() > lifetimeHigh) {
                lifetimeHigh = candle.high();
            }

            boolean isGreenCandle = candle.close() > candle.open();
            if (isGreenCandle) {
                if (!sequenceStarted) {
                    sequenceEntryLow = candle.low();
                    sequenceHigh = candle.high();
                    sequenceStarted = true;
                    currentSequenceStartDate = candle.time();
                    firstGreenCandleSma200 = sma(closes, 200);
                } else {
                    sequenceHigh = Math.max(sequenceHigh, candle.high());
                }
            } else {
                sequenceEntryLow = null;
                sequenceHigh = null;
                sequenceStarted = false;
                currentSequenceStartDate = null;
                firstGreenCandleSma200 = null;
            }

            if (sequenceStarted && sequenceEntryLow != null && sequenceHigh != null) {
                double percentageMove = ((sequenceHigh - sequenceEntryLow) / sequenceEntryLow) * 100.0;
                boolean conditionMet = percentageMove >= minPercentageMove;
                if (conditionMet) {
                    boolean passesV200StartRule = !"V200".equalsIgnoreCase(stock.group())
                        || (firstGreenCandleSma200 != null && sequenceEntryLow < firstGreenCandleSma200);
                    if (passesV200StartRule) {
                        latestFormationMove = percentageMove;
                        latestFormationLow = sequenceEntryLow;
                        latestFormationHigh = sequenceHigh;
                        latestFormationStartDate = currentSequenceStartDate;
                        latestFormationEndDate = candle.time();
                        latestFormationEndIndex = candleIndex;
                    }
                    sequenceEntryLow = null;
                    sequenceHigh = null;
                    sequenceStarted = false;
                    currentSequenceStartDate = null;
                    firstGreenCandleSma200 = null;
                }
            }
        }

        Candle latest = candles.getLast();
        boolean entryTriggered = false;
        boolean exitTriggered = false;
        String entryTriggerDate = null;
        String exitTriggerDate = null;

        if (latestFormationLow != null && latestFormationHigh != null && latestFormationEndIndex >= 0) {
            for (int index = latestFormationEndIndex + 1; index < candles.size(); index++) {
                Candle candle = candles.get(index);
                if (!entryTriggered && candle.low() <= latestFormationLow) {
                    entryTriggered = true;
                    entryTriggerDate = candle.time();
                }

                if (entryTriggered && candle.high() >= latestFormationHigh) {
                    exitTriggered = true;
                    exitTriggerDate = candle.time();
                    break;
                }
            }
        }

        String signal = "NONE";
        String signalDate = latestFormationEndDate;
        if (entryTriggered && !exitTriggered) {
            signal = "BUY";
            signalDate = entryTriggerDate;
        } else if (entryTriggered) {
            signal = "SELL";
            signalDate = exitTriggerDate;
        }

        String notes;
        if (latestFormationLow == null || latestFormationHigh == null) {
            notes = "No V20 sequence hit the threshold on the available history.";
        } else if (!entryTriggered) {
            notes = "Latest V20 setup exists, but price has never revisited the entry level after the formation.";
        } else if (!exitTriggered) {
            notes = "Latest V20 setup is active because price revisited the entry level and has not yet reached the target.";
        } else {
            notes = "Latest V20 setup completed because price revisited the entry level first and later reached the target.";
        }

        double percentBelowLifetimeHigh = lifetimeHigh > 0
            ? ((lifetimeHigh - latest.close()) / lifetimeHigh) * 100.0
            : 0.0;
        Double sma200 = sma(closes, 200);
        Double high52Week = rollingExtreme(highs, 260, true);
        Double low52Week = rollingExtreme(lows, 260, false);

        return new ScannerResult(
            StrategyType.V20.slug(),
            stock.symbol(),
            stock.yahooSymbol(),
            signal,
            StrategyType.V20.displayName(),
            signalDate,
            latest.close(),
            null,
            null,
            sma200,
            latestFormationMove,
            latestFormationLow,
            latestFormationHigh,
            latestFormationStartDate,
            latestFormationEndDate,
            percentBelowLifetimeHigh,
            high52Week,
            low52Week,
            null,
            false,
            false,
            OffsetDateTime.now().toString(),
            notes
        );
    }

    private ScannerResult evaluateMultibagger(WatchlistStock stock, List<Candle> candles) {
        List<Double> closes = new ArrayList<>();
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        for (Candle candle : candles) {
            closes.add(candle.close());
            highs.add(candle.high());
            lows.add(candle.low());
        }
        if (closes.size() < 200) {
            throw new IllegalStateException("not enough history to calculate multibagger trend score");
        }

        Candle latest = candles.getLast();
        double lastClose = latest.close();
        Double sma50 = sma(closes, 50);
        Double sma200 = sma(closes, 200);
        Double high52Week = rollingExtreme(highs, 260, true);
        Double low52Week = rollingExtreme(lows, 260, false);
        Double oneMonthReturn = percentChangeFromLookback(closes, 21);
        Double threeMonthReturn = percentChangeFromLookback(closes, 63);
        Double sixMonthReturn = percentChangeFromLookback(closes, 126);
        Double twelveMonthReturn = percentChangeFromLookback(closes, 252);
        Double averageTurnover = averageTurnover(candles, 20);
        double percentBelow52WeekHigh = high52Week != null && high52Week > 0
            ? ((high52Week - lastClose) / high52Week) * 100.0
            : 0.0;

        int score = 0;
        List<String> points = new ArrayList<>();
        List<String> checks = new ArrayList<>();

        if (sma200 != null && lastClose > sma200) {
            score += 15;
            points.add("price is above 200-DMA");
        }
        if (sma50 != null && sma200 != null && sma50 > sma200) {
            score += 10;
            points.add("50-DMA is above 200-DMA");
        }
        if (sixMonthReturn != null && sixMonthReturn >= 40.0) {
            score += 15;
            points.add("6M price growth is " + formatPercent(sixMonthReturn));
        } else if (sixMonthReturn != null && sixMonthReturn >= 25.0) {
            score += 10;
            points.add("6M price growth is " + formatPercent(sixMonthReturn));
        }
        if (twelveMonthReturn != null && twelveMonthReturn >= 60.0) {
            score += 10;
            points.add("12M price growth is " + formatPercent(twelveMonthReturn));
        } else if (twelveMonthReturn != null && twelveMonthReturn >= 35.0) {
            score += 6;
            points.add("12M price growth is " + formatPercent(twelveMonthReturn));
        }
        if (threeMonthReturn != null && threeMonthReturn >= 15.0) {
            score += 8;
            points.add("3M momentum is " + formatPercent(threeMonthReturn));
        }
        if (oneMonthReturn != null && oneMonthReturn > 0.0) {
            score += 7;
            points.add("1M momentum remains positive");
        }
        if (percentBelow52WeekHigh <= 15.0) {
            score += 10;
            points.add("within " + formatPercent(percentBelow52WeekHigh) + " of 52-week high");
        } else if (percentBelow52WeekHigh <= 25.0) {
            score += 6;
            points.add("within " + formatPercent(percentBelow52WeekHigh) + " of 52-week high");
        }
        if (high52Week != null && low52Week != null && low52Week > 0 && high52Week / low52Week >= 1.6) {
            score += 5;
            points.add("52-week range shows rerating potential");
        }
        if (averageTurnover != null && averageTurnover >= 50_000_000.0) {
            score += 10;
            points.add("20D average traded value is above Rs 5 crore");
        } else if (averageTurnover != null && averageTurnover >= 10_000_000.0) {
            score += 6;
            points.add("20D average traded value is above Rs 1 crore");
        } else {
            checks.add("liquidity is thin; entry/exit risk needs manual check");
        }
        if (sma50 != null && lastClose > sma50) {
            score += 10;
            points.add("price is above 50-DMA");
        } else {
            checks.add("near-term trend is weak versus 50-DMA");
        }

        String signal = score >= 70 ? "ALERT" : score >= 55 ? "WATCH" : "NONE";
        String notes = "Score " + score + "/100. Points: "
            + (points.isEmpty() ? "no strong multibagger-screen factors yet" : String.join("; ", points))
            + ". Checks: "
            + (checks.isEmpty()
            ? "verify sales/PAT acceleration, cash conversion, pledges, valuation, and source filings before acting"
            : String.join("; ", checks) + "; verify sales/PAT acceleration, cash conversion, pledges, valuation, and source filings before acting")
            + ".";

        return new ScannerResult(
            StrategyType.MULTIBAGGER.slug(),
            stock.symbol(),
            stock.yahooSymbol(),
            signal,
            StrategyType.MULTIBAGGER.displayName(),
            signal.equals("NONE") ? null : latest.time(),
            lastClose,
            null,
            sma50,
            sma200,
            sixMonthReturn,
            null,
            null,
            null,
            null,
            percentBelow52WeekHigh,
            high52Week,
            low52Week,
            (double) score,
            false,
            false,
            OffsetDateTime.now().toString(),
            notes
        );
    }

    private Double sma(List<Double> closes, int period) {
        if (closes.size() < period) {
            return null;
        }
        double sum = 0;
        for (int index = closes.size() - period; index < closes.size(); index++) {
            sum += closes.get(index);
        }
        return sum / period;
    }

    private Double percentChangeFromLookback(List<Double> closes, int sessions) {
        if (closes.size() <= sessions) {
            return null;
        }
        double previous = closes.get(closes.size() - 1 - sessions);
        if (previous <= 0) {
            return null;
        }
        return ((closes.getLast() - previous) / previous) * 100.0;
    }

    private Double averageTurnover(List<Candle> candles, int sessions) {
        if (candles.isEmpty()) {
            return null;
        }
        int start = Math.max(0, candles.size() - sessions);
        double total = 0;
        int count = 0;
        for (int index = start; index < candles.size(); index++) {
            Candle candle = candles.get(index);
            if (candle.volume() <= 0) {
                continue;
            }
            total += candle.close() * candle.volume();
            count++;
        }
        return count == 0 ? null : total / count;
    }

    private String formatPercent(double value) {
        return String.format("%.1f%%", value);
    }

    private String isoFromUnix(long unixSeconds) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(unixSeconds), ZoneOffset.UTC).toString();
    }

    private Double rollingExtreme(List<Double> values, int period, boolean highest) {
        if (values.isEmpty()) {
            return null;
        }
        int start = Math.max(0, values.size() - period);
        double extreme = values.get(start);
        for (int index = start + 1; index < values.size(); index++) {
            extreme = highest ? Math.max(extreme, values.get(index)) : Math.min(extreme, values.get(index));
        }
        return extreme;
    }

    private record Candle(String time, double open, double high, double low, double close, long volume) {
    }

    private record ScanBatchResult(List<ScannerResult> results, List<ScannerFailure> failures) {
    }
}
