package com.hackathon.emergency108.controller;

import com.hackathon.emergency108.repository.UserRepository;
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
        return "Emergency 108 backend is running";
    }

    @GetMapping("/api/users/count")
    public long usersCount() {
        return userRepository.count();
    }
}

