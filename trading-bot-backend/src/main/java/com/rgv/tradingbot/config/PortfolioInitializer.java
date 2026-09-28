package com.rgv.tradingbot.config;

import com.rgv.tradingbot.model.Portfolio;
import com.rgv.tradingbot.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioInitializer implements ApplicationRunner {

    private static final long DEFAULT_PORTFOLIO_ID = 1L;

    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (portfolioRepository.existsById(DEFAULT_PORTFOLIO_ID)) {
            return;
        }

        Portfolio portfolio = new Portfolio(0);
        portfolio.setId(DEFAULT_PORTFOLIO_ID);
        portfolioRepository.save(portfolio);
        log.info("Created default portfolio {} with zero starting balance", DEFAULT_PORTFOLIO_ID);
    }
}
