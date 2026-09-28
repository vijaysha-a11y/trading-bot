package com.rgv.tradingbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double cashBalance;
    private double holdingsQuantity;   // simplified: quantity of the one asset being traded
    private LocalDateTime updatedAt;

    public Portfolio() {}

    public Portfolio(double cashBalance) {
        this.cashBalance = cashBalance;
        this.holdingsQuantity = 0;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
        this.updatedAt = LocalDateTime.now();
    }

    public double getHoldingsQuantity() { return holdingsQuantity; }
    public void setHoldingsQuantity(double holdingsQuantity) { this.holdingsQuantity = holdingsQuantity; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}