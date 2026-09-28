package com.rgv.tradingbot.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeOrderRequest {
    private String symbol;
    private String action;
    private String orderType;
    private BigDecimal price;
    private BigDecimal limitPrice;
    private BigDecimal quantity;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private String strategyName;
    private Double confidence;
    private String reason;
}
