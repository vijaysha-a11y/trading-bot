package com.rgv.tradingbot.repository;

import com.rgv.tradingbot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findBySymbolOrderByTimestampDesc(String symbol);
    List<Trade> findAllByOrderByTimestampDesc();

    @Query("SELECT t FROM Trade t WHERE t.portfolioId = :portfolioId " +
            "OR (t.portfolioId IS NULL AND :portfolioId = 1) ORDER BY t.timestamp DESC")
    List<Trade> findAllForPortfolio(@Param("portfolioId") Long portfolioId);
}