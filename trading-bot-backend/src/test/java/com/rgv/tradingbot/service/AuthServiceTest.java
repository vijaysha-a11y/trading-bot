package com.rgv.tradingbot.service;

import com.rgv.tradingbot.controller.AuthController.RegistrationRequest;
import com.rgv.tradingbot.model.AppUser;
import com.rgv.tradingbot.model.Portfolio;
import com.rgv.tradingbot.repository.AppUserRepository;
import com.rgv.tradingbot.repository.PortfolioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AppUserRepository userRepository;
    private PortfolioRepository portfolioRepository;
    private PasswordEncoder passwordEncoder;
    private SecurityContextRepository securityContextRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        portfolioRepository = mock(PortfolioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        securityContextRepository = mock(SecurityContextRepository.class);
        authService = new AuthService(
                userRepository,
                portfolioRepository,
                passwordEncoder,
                authenticationManager,
                securityContextRepository);
        when(passwordEncoder.encode("correct-horse")).thenReturn("encoded-password");
        when(portfolioRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(portfolio(1L)));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void firstRegisteredAccountBecomesAdminAndUsesDefaultPortfolio() {
        when(userRepository.count()).thenReturn(0L);

        var response = authService.register(
                new RegistrationRequest("First Owner", "owner@example.com", "correct-horse"),
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class));

        assertEquals("ADMIN", response.role());
        assertEquals(1L, response.portfolioId());
        verify(portfolioRepository, never()).save(any(Portfolio.class));
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    void laterRegistrationsBecomeUsersWithTheirOwnPortfolio() {
        when(userRepository.count()).thenReturn(1L);
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> {
            Portfolio portfolio = invocation.getArgument(0);
            portfolio.setId(2L);
            return portfolio;
        });

        var response = authService.register(
                new RegistrationRequest("Second Member", "member@example.com", "correct-horse"),
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class));

        assertEquals("USER", response.role());
        assertEquals(2L, response.portfolioId());
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    private Portfolio portfolio(Long id) {
        Portfolio portfolio = new Portfolio(10_000);
        portfolio.setId(id);
        return portfolio;
    }
}
