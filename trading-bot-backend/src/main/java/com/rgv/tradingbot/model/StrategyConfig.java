package com.rgv.tradingbot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "strategy_config")
public class StrategyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name = "RSI_MEAN_REVERSION";
    private String symbol = "BTCUSDT";
    private int shortMaPeriod = 20;
    private int longMaPeriod = 50;
    private int rsiPeriod = 14;
    private double rsiBuyThreshold = 30;
    private double rsiSellThreshold = 70;
    private boolean active = true;

    public StrategyConfig() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getShortMaPeriod() { return shortMaPeriod; }
    public void setShortMaPeriod(int shortMaPeriod) { this.shortMaPeriod = shortMaPeriod; }

    public int getLongMaPeriod() { return longMaPeriod; }
    public void setLongMaPeriod(int longMaPeriod) { this.longMaPeriod = longMaPeriod; }

    public int getRsiPeriod() { return rsiPeriod; }
    public void setRsiPeriod(int rsiPeriod) { this.rsiPeriod = rsiPeriod; }

    public double getRsiBuyThreshold() { return rsiBuyThreshold; }
    public void setRsiBuyThreshold(double rsiBuyThreshold) { this.rsiBuyThreshold = rsiBuyThreshold; }

    public double getRsiSellThreshold() { return rsiSellThreshold; }
    public void setRsiSellThreshold(double rsiSellThreshold) { this.rsiSellThreshold = rsiSellThreshold; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}