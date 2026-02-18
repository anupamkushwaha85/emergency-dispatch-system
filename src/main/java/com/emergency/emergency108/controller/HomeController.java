package com.emergency.emergency108.controller;

import com.emergency.emergency108.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home() {
        return "OK";
    }

    @GetMapping("/api/users/count")
    public long usersCount() {
        return userRepository.count();
    }
}
