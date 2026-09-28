package com.rgv.tradingbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled annotation support, required for
 * TradingScheduler's fixedDelay jobs to actually run.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}