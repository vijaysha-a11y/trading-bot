package com.rgv.tradingbot.repository;

import com.rgv.tradingbot.model.PriceCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceCandleRepository extends JpaRepository<PriceCandle, Long> {
    List<PriceCandle> findTop100BySymbolOrderByTimestampDesc(String symbol);
    List<PriceCandle> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            String symbol, LocalDateTime start, LocalDateTime end);
    Optional<PriceCandle> findFirstBySymbolAndTimestamp(String symbol, LocalDateTime timestamp);
}