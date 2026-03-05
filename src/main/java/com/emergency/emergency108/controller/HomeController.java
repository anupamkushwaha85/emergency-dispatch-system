package com.emergency.emergency108.controller;

import com.emergency.emergency108.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing Home endpoints.
 *
 * @author anupam kushwaha
 */
@RestController
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Home operation.
     * @return the String
     */
    @GetMapping("/")
    public String home() {
        return "OK";
    }

    /**
     * Users count operation.
     * @return the long
     */
    @GetMapping("/api/users/count")
    public long usersCount() {
        return userRepository.count();
    }
}
