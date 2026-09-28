package com.rgv.tradingbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgv.tradingbot.model.PriceCandle;
import com.rgv.tradingbot.repository.PriceCandleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class MarketDataService {

    private final PriceCandleRepository priceCandleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String marketDataApiBaseUrl;

    public MarketDataService(
            PriceCandleRepository priceCandleRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${trading.market-data.base-url:}") String marketDataApiBaseUrl) {

        this.priceCandleRepository = priceCandleRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.marketDataApiBaseUrl = marketDataApiBaseUrl;
    }

    public PriceCandle fetchLatestCandle(String symbol) {

        if (marketDataApiBaseUrl.isBlank()) {
            log.warn("Market data provider is not configured; skipping {}", symbol);
            return null;
        }

        try {
            String url = marketDataApiBaseUrl
                    + "/api/v3/klines"
                    + "?symbol=" + symbol
                    + "&interval=1m"
                    + "&limit=1";

            log.info("Fetching market data from: {}", url);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isBlank()) {
                log.warn("No market data returned for {}", symbol);
                return null;
            }

            JsonNode root = objectMapper.readTree(response);

            if (!root.isArray() || root.isEmpty()) {
                log.warn("Invalid market data response for {}", symbol);
                return null;
            }

            JsonNode candleData = root.get(0);

            double open = candleData.get(1).asDouble();
            double high = candleData.get(2).asDouble();
            double low = candleData.get(3).asDouble();
            double close = candleData.get(4).asDouble();
            double volume = candleData.get(5).asDouble();

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(candleData.get(0).asLong()), ZoneId.systemDefault());
            PriceCandle candle = priceCandleRepository.findFirstBySymbolAndTimestamp(symbol, timestamp)
                    .orElseGet(PriceCandle::new);
            candle.setSymbol(symbol);
            candle.setTimestamp(timestamp);
            candle.setOpen(open);
            candle.setHigh(high);
            candle.setLow(low);
            candle.setClose(close);
            candle.setVolume(volume);

            PriceCandle saved = priceCandleRepository.save(candle);

            log.info(
                    "Market data saved: {} | Open: {} | High: {} | Low: {} | Close: {} | Volume: {}",
                    symbol,
                    open,
                    high,
                    low,
                    close,
                    volume
            );

            return saved;

        } catch (Exception ex) {
            log.error(
                    "Failed to fetch market data for {}: {}",
                    symbol,
                    ex.getMessage(),
                    ex
            );
            return null;
        }
    }

    public List<PriceCandle> getRecentCandles(String symbol, int count) {

        List<PriceCandle> latestFirst =
                priceCandleRepository.findTop100BySymbolOrderByTimestampDesc(symbol);

        List<PriceCandle> limited =
                latestFirst.size() > count
                        ? latestFirst.subList(0, count)
                        : latestFirst;

        List<PriceCandle> oldestFirst =
                new ArrayList<>(limited);

        Collections.reverse(oldestFirst);

        return oldestFirst;
    }
}