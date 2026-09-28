package com.rgv.tradingbot.exception;

/**
 * Thrown when a withdrawal, trade, or order execution is attempted
 * with insufficient available balance in the portfolio/fund.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}