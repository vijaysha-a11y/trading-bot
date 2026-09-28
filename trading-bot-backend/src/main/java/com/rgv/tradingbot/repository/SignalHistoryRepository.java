package com.rgv.tradingbot.repository;

import com.rgv.tradingbot.model.SignalHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalHistoryRepository extends JpaRepository<SignalHistoryEntry, Long> {
    boolean existsByEventKey(String eventKey);
    List<SignalHistoryEntry> findTop50BySymbolOrderByGeneratedAtDesc(String symbol);
}
