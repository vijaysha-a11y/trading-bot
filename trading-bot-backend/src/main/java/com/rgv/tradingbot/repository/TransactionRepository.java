package com.rgv.tradingbot.repository;

import com.rgv.tradingbot.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByTimestampDesc();

    @Query("SELECT t FROM Transaction t WHERE t.portfolioId = :portfolioId " +
            "OR (t.portfolioId IS NULL AND :portfolioId = 1) ORDER BY t.timestamp DESC")
    List<Transaction> findAllForPortfolio(@Param("portfolioId") Long portfolioId);
}