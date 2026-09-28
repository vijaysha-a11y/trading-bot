package com.rgv.tradingbot.repository;

import com.rgv.tradingbot.model.PaperOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PaperOrderRepository extends JpaRepository<PaperOrder, Long> {
    List<PaperOrder> findByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);
    List<PaperOrder> findByStatusInOrderByCreatedAtAsc(Collection<String> statuses);
}
