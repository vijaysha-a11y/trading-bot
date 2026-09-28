package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.model.SignalHistoryEntry;
import com.rgv.tradingbot.repository.SignalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignalHistoryService {
    private final SignalHistoryRepository repository;

    @Transactional
    public void record(List<TradeSignalDTO> signals) {
        for (TradeSignalDTO signal : signals) {
            if (signal.getGeneratedAt() == null) signal.setGeneratedAt(LocalDateTime.now());
            LocalDateTime minute = signal.getGeneratedAt().truncatedTo(ChronoUnit.MINUTES);
            String eventKey = String.join("|", signal.getSymbol(), signal.getStrategyName(),
                    signal.getAction(), minute.toString());
            if (repository.existsByEventKey(eventKey)) continue;
            SignalHistoryEntry entry = new SignalHistoryEntry();
            entry.setEventKey(eventKey);
            entry.setSymbol(signal.getSymbol());
            entry.setAction(signal.getAction());
            entry.setStrategyName(signal.getStrategyName());
            entry.setReason(signal.getReason());
            entry.setConfidence(signal.getConfidence());
            entry.setPrice(signal.getPrice() == null ? null : signal.getPrice().doubleValue());
            entry.setGeneratedAt(signal.getGeneratedAt());
            repository.save(entry);
        }
    }

    @Transactional(readOnly = true)
    public List<SignalHistoryEntry> getRecent(String symbol) {
        return repository.findTop50BySymbolOrderByGeneratedAtDesc(symbol);
    }
}
