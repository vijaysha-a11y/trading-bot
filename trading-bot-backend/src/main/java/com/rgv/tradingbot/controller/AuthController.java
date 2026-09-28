package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.dto.AccountResponse;
import com.rgv.tradingbot.model.AppUser;
import com.rgv.tradingbot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(
            @RequestBody RegistrationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return authService.register(request, servletRequest, servletResponse);
    }

    @PostMapping("/login")
    public AccountResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return authService.login(request, servletRequest, servletResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> currentUser(@AuthenticationPrincipal AppUser user) {
        if (user == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(AccountResponse.from(user));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
    }

    public record CsrfResponse(String token) {}
    public record RegistrationRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}
}
