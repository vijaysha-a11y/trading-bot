package com.rgv.tradingbot.dto;

import com.rgv.tradingbot.model.AppUser;

public record AccountResponse(Long id, String name, String email, String role, Long portfolioId) {
    public static AccountResponse from(AppUser user) {
        return new AccountResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getPortfolioId());
    }
}
