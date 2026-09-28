package com.rgv.tradingbot.service;

import com.rgv.tradingbot.controller.AuthController.LoginRequest;
import com.rgv.tradingbot.controller.AuthController.RegistrationRequest;
import com.rgv.tradingbot.dto.AccountResponse;
import com.rgv.tradingbot.model.AccountRole;
import com.rgv.tradingbot.model.AppUser;
import com.rgv.tradingbot.model.Portfolio;
import com.rgv.tradingbot.repository.AppUserRepository;
import com.rgv.tradingbot.repository.PortfolioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AppUserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(
            AppUserRepository userRepository,
            PortfolioRepository portfolioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public AccountResponse register(
            RegistrationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.email() == null || !EMAIL_PATTERN.matcher(request.email().trim()).matches()
                || request.password() == null || request.password().length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enter your name, a valid email, and a password of at least 8 characters.");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        var firstPortfolio = portfolioRepository.findByIdForUpdate(1L)
                .orElseThrow(() -> new IllegalStateException("Default portfolio has not been initialized."));
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        boolean firstAccount = userRepository.count() == 0;
        Portfolio portfolio = firstAccount ? firstPortfolio : portfolioRepository.save(new Portfolio(0));
        AccountRole role = firstAccount ? AccountRole.ADMIN : AccountRole.USER;
        AppUser user = userRepository.save(new AppUser(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role,
                portfolio.getId()));

        establishSession(user, servletRequest, servletResponse);
        return AccountResponse.from(user);
    }

    public AccountResponse login(
            LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim().toLowerCase(Locale.ROOT), request.password()));
        } catch (org.springframework.security.core.AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }

        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return AccountResponse.from((AppUser) authentication.getPrincipal());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(
                SecurityContextHolder.createEmptyContext(), request, response);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    private void establishSession(
            AppUser user, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
    }
}
