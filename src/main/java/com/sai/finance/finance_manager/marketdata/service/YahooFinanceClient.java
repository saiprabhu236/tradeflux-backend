package com.sai.finance.finance_manager.marketdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.finance.finance_manager.marketdata.dto.*;
import com.sai.finance.finance_manager.marketdata.util.SymbolMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Component
public class YahooFinanceClient {

    // ---------------------------------------------------------
    // CACHES (KEEPING YOUR EXISTING CACHE STRUCTURE)
    // ---------------------------------------------------------
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private final Map<String, CachedHistorical> historicalCache = new ConcurrentHashMap<>();
    private final Map<String, CachedMetrics> metricsCache = new ConcurrentHashMap<>();

    // ---------------------------------------------------------
    // YAHOO ENDPOINTS (UNCHANGED)
    // ---------------------------------------------------------
    private static final String PRICE_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1m";

    private static final String CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=%s";

    private static final String METRICS_URL =
            "https://query2.finance.yahoo.com/v10/finance/quoteSummary/%s?modules=summaryDetail";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // ---------------------------------------------------------
    // VALIDATION (UNCHANGED)
    // ---------------------------------------------------------
    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        if (!symbol.matches("^[A-Za-z0-9.^-]+$")) {
            throw new IllegalArgumentException("Invalid symbol format: " + symbol);
        }
    }

    // ---------------------------------------------------------
    // RETRY WRAPPER (UNCHANGED)
    // ---------------------------------------------------------


    // ---------------------------------------------------------
    // 1️⃣ CURRENT PRICE (UPDATED WITH TTL + FALLBACK)
    // ---------------------------------------------------------
    public PriceDto getCurrentPrice(String rawSymbol) {
        String symbol = SymbolMapper.toYahooSymbol(rawSymbol);
        validateSymbol(symbol);

        try {
            // CACHE (TTL = 5 seconds)
            CachedPrice cached = priceCache.get(symbol);
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 5000) {
                return cached.price;
            }

            // DIRECT FETCH (no retry)
            PriceDto dto = fetchPrice(symbol);

            // CACHE RESULT
            priceCache.put(symbol, new CachedPrice(dto, System.currentTimeMillis()));

            return dto;

        } catch (Exception ex) {
            log.error("Error fetching price for {}", rawSymbol, ex);

            // FALLBACK
            CachedPrice cached = priceCache.get(symbol);
            if (cached != null) {
                return cached.price;
            }

            return new PriceDto(rawSymbol, 0, 0, 0, 0);
        }
    }

    private PriceDto fetchPrice(String symbol) {
        String url = PRICE_URL.formatted(symbol);
        log.info("Fetching price → {}", url);

        try {
            // Browser-like headers to avoid Yahoo 429
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            headers.add("Accept", "application/json, text/plain, */*");
            headers.add("Accept-Language", "en-US,en;q=0.9");
            headers.add("Connection", "keep-alive");
            headers.add("Referer", "https://finance.yahoo.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Extract JSON body
            String body = response.getBody();

            // Parse JSON
            JsonNode root = mapper.readTree(body);

            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode meta = result.path("meta");
            JsonNode indicators = result.path("indicators").path("quote").get(0);

            double current = indicators.path("close").get(0).asDouble(0.0);
            double prevClose = meta.path("previousClose").asDouble(0.0);

            double change = current - prevClose;
            double changePct = prevClose != 0 ? (change / prevClose) * 100 : 0;

            return new PriceDto(
                    symbol.replace(".NS", ""),
                    current,
                    prevClose,
                    change,
                    changePct
            );

        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch or parse Yahoo price JSON", ex);
        }
    }

    // ---------------------------------------------------------
    // 2️⃣ HISTORICAL DATA (UPDATED WITH TTL + FALLBACK)
    // ---------------------------------------------------------
    public List<CandleDto> getHistoricalData(String rawSymbol, String range, String interval) {
        String symbol = SymbolMapper.toYahooSymbol(rawSymbol);
        validateSymbol(symbol);

        String cacheKey = symbol + "_" + range + "_" + interval;

        try {
            // CACHE (TTL = 60 seconds)
            CachedHistorical cached = historicalCache.get(cacheKey);
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 60000) {
                return cached.candles;
            }

            // FETCH WITH RETRY
            List<CandleDto> candles = fetchHistorical(symbol, range, interval);

            // CACHE RESULT
            historicalCache.put(cacheKey, new CachedHistorical(candles, System.currentTimeMillis()));

            return candles;

        } catch (Exception ex) {
            log.error("Error fetching historical data for {}", rawSymbol, ex);

            // FALLBACK
            CachedHistorical cached = historicalCache.get(cacheKey);
            if (cached != null) {
                return cached.candles;
            }

            return List.of();
        }
    }

    public List<CandleDto> fetchHistorical(String symbol, String range, String interval) {
        String url = CHART_URL.formatted(symbol, range, interval);
        log.info("Fetching historical → {}", url);

        // --- 1. Add browser-like headers to avoid Yahoo bot-blocking ---
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        headers.add("Accept", "application/json");
        headers.add("Connection", "keep-alive");
        //headers.add("Accept-Encoding", "gzip");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        String body = response.getBody();

        // --- 2. Parse JSON safely ---
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Yahoo historical JSON", ex);
        }

        JsonNode results = root.path("chart").path("result");

        // --- 3. Guard against null or empty results ---
        if (results.isMissingNode() || results.isNull() || results.size() == 0) {
            throw new RuntimeException("Yahoo returned no historical data for " + symbol);
        }

        JsonNode result = results.get(0);

        JsonNode timestamps = result.path("timestamp");
        JsonNode indicators = result.path("indicators").path("quote").get(0);

        List<CandleDto> candles = new ArrayList<>();

        // --- 4. Build candle list safely ---
        for (int i = 0; i < timestamps.size(); i++) {
            candles.add(new CandleDto(
                    timestamps.get(i).asLong(),
                    indicators.path("open").get(i).asDouble(0.0),
                    indicators.path("high").get(i).asDouble(0.0),
                    indicators.path("low").get(i).asDouble(0.0),
                    indicators.path("close").get(i).asDouble(0.0),
                    indicators.path("volume").get(i).asLong(0L)
            ));
        }

        return candles;
    }

    // ---------------------------------------------------------
    // 3️⃣ STOCK METRICS (UPDATED WITH TTL + FALLBACK)
    // ---------------------------------------------------------
    public StockMetricsDto getStockMetrics(String rawSymbol) {
        String symbol = SymbolMapper.toYahooSymbol(rawSymbol);
        validateSymbol(symbol);

        try {
            // CACHE (TTL = 60 seconds)
            CachedMetrics cached = metricsCache.get(symbol);
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 60000) {
                return cached.metrics;
            }


            StockMetricsDto dto = fetchMetrics(symbol);

            // CACHE RESULT
            metricsCache.put(symbol, new CachedMetrics(dto, System.currentTimeMillis()));

            return dto;

        } catch (Exception ex) {
            log.error("Error fetching metrics for {}", rawSymbol, ex);

            // FALLBACK
            CachedMetrics cached = metricsCache.get(symbol);
            if (cached != null) {
                return cached.metrics;
            }

            return new StockMetricsDto(rawSymbol, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public StockMetricsDto fetchMetrics(String symbol) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=1d"
                .formatted(symbol);

        log.info("Fetching metrics → {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.add("Accept", "application/json");
            headers.add("Connection", "keep-alive");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = mapper.readTree(response.getBody());

            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode meta = result.path("meta");
            JsonNode quote = result.path("indicators").path("quote").get(0);

            return new StockMetricsDto(
                    symbol.replace(".NS", ""),

                    // OPEN
                    quote.path("open").get(0).asDouble(0.0),

                    // HIGH
                    quote.path("high").get(0).asDouble(0.0),

                    // LOW
                    quote.path("low").get(0).asDouble(0.0),

                    // CLOSE (current price)
                    quote.path("close").get(0).asDouble(0.0),

                    // VOLUME
                    quote.path("volume").get(0).asLong(0L),

                    // 52-WEEK HIGH
                    meta.path("fiftyTwoWeekHigh").asDouble(0.0),

                    // 52-WEEK LOW
                    meta.path("fiftyTwoWeekLow").asDouble(0.0)
            );

        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Yahoo metrics JSON", ex);
        }
    }

    // ---------------------------------------------------------
    // CACHE CLASSES (UNCHANGED)
    // ---------------------------------------------------------
    private static class CachedPrice {
        PriceDto price;
        long timestamp;
        CachedPrice(PriceDto price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    private static class CachedHistorical {
        List<CandleDto> candles;
        long timestamp;
        CachedHistorical(List<CandleDto> candles, long timestamp) {
            this.candles = candles;
            this.timestamp = timestamp;
        }
    }

    private static class CachedMetrics {
        StockMetricsDto metrics;
        long timestamp;
        CachedMetrics(StockMetricsDto metrics, long timestamp) {
            this.metrics = metrics;
            this.timestamp = timestamp;
        }
    }
    public double fetchPriceValue(String symbol) {
        PriceDto dto = fetchPrice(symbol);
        return dto != null ? dto.getCurrentPrice() : 0.0;
    }

}