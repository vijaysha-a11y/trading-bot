package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.dto.AccountResponse;
import com.rgv.tradingbot.repository.AppUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AppUserRepository userRepository;

    public AdminController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<AccountResponse> users() {
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(AccountResponse::from)
                .toList();
    }
}
