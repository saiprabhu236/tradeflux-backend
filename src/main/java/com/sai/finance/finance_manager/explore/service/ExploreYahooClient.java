package com.sai.finance.finance_manager.explore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.sai.finance.finance_manager.explore.model.Candle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ExploreYahooClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public record YahooQuote(
            double previousClose,
            double lastPrice,
            long volume,
            long averageVolume,
            double fiftyTwoWeekHigh,
            double fiftyTwoWeekLow
    ) {}



    public YahooQuote fetchQuote(String symbol) {

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol)
                    .queryParam("interval", "1m")
                    .queryParam("range", "1d")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.add("Accept", "application/json, text/plain, */*");
            headers.add("Accept-Language", "en-US,en;q=0.9");
            headers.add("Connection", "keep-alive");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Bad response from Yahoo for {}", symbol);
                return null;
            }

            Map<String, Object> body = response.getBody();
            Map<String, Object> chart = (Map<String, Object>) body.get("chart");
            if (chart == null) return null;

            var resultList = (java.util.List<Map<String, Object>>) chart.get("result");
            if (resultList == null || resultList.isEmpty()) return null;

            Map<String, Object> result = resultList.get(0);
            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            if (meta == null) return null;

            double previousClose = toDouble(meta.get("previousClose"));
            double lastPrice = toDouble(meta.get("regularMarketPrice"));

            long volume = toLong(meta.get("regularMarketVolume"));
            long averageVolume = toLong(meta.get("averageDailyVolume10Day"));

            double fiftyTwoWeekHigh = toDouble(meta.get("fiftyTwoWeekHigh"));
            double fiftyTwoWeekLow = toDouble(meta.get("fiftyTwoWeekLow"));

            if (lastPrice <= 0) {
                //log.warn("Invalid price for {} → {}", symbol, lastPrice);
                return null;
            }

            return new YahooQuote(
                    previousClose,
                    lastPrice,
                    volume,
                    averageVolume,
                    fiftyTwoWeekHigh,
                    fiftyTwoWeekLow
            );

        } catch (Exception e) {
            //log.error("Yahoo fetch failed for {} → {}", symbol, e.getMessage());
            return null;
        }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private long toLong(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Candle> getDailyCandles(String symbol, int years) {
        try {
            long period2 = System.currentTimeMillis() / 1000;
            long period1 = period2 - (years * 365L * 24 * 60 * 60);

            String url = String.format(
                    "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&period1=%d&period2=%d",
                    symbol, period1, period2
            );

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();

            Map chart = (Map) response.get("chart");
            if (chart == null) return List.of();

            List<Map> resultList = (List<Map>) chart.get("result");
            if (resultList == null || resultList.isEmpty()) return List.of();

            Map result = resultList.get(0);

            List<Integer> timestamps = (List<Integer>) result.get("timestamp");
            if (timestamps == null) return List.of();

            Map indicators = (Map) result.get("indicators");
            Map quote = (Map) ((List) indicators.get("quote")).get(0);

            List<Double> opens = (List<Double>) quote.get("open");
            List<Double> highs = (List<Double>) quote.get("high");
            List<Double> lows = (List<Double>) quote.get("low");
            List<Double> closes = (List<Double>) quote.get("close");
            List<Long> volumes = (List<Long>) quote.get("volume");

            List<Candle> candles = new ArrayList<>();

            for (int i = 0; i < timestamps.size(); i++) {
                if (closes.get(i) == null) continue; // skip null days

                candles.add(new Candle(
                        timestamps.get(i) * 1000L,
                        opens.get(i),
                        highs.get(i),
                        lows.get(i),
                        closes.get(i),
                        volumes.get(i)
                ));
            }

            return candles;

        } catch (Exception e) {
            return List.of();
        }
    }
}