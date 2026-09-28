package com.rgv.tradingbot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "trades")

public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private TradeType type;

    private double quantity;
    private double price;
    private double pnl;
    private String strategyReason;
    private LocalDateTime timestamp;
    private Long portfolioId;

    public Trade() {}

    public Trade(String symbol, TradeType type, double quantity, double price, String strategyReason) {
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.strategyReason = strategyReason;
        this.timestamp = LocalDateTime.now();
    }

    public Trade(String symbol, TradeType type, double quantity, double price, String strategyReason, Long portfolioId) {
        this(symbol, type, quantity, price, strategyReason);
        this.portfolioId = portfolioId;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public TradeType getType() { return type; }
    public void setType(TradeType type) { this.type = type; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getPnl() { return pnl; }
    public void setPnl(double pnl) { this.pnl = pnl; }

    public String getStrategyReason() { return strategyReason; }
    public void setStrategyReason(String strategyReason) { this.strategyReason = strategyReason; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
}