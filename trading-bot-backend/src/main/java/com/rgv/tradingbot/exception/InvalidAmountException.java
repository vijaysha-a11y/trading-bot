package com.rgv.tradingbot.exception;

/**
 * Thrown when a fund deposit/withdrawal amount, or an order quantity/price,
 * is invalid (e.g. negative, zero, or exceeds allowed limits).
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}