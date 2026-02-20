package com.emergency.emergency108;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class })
public class Emergency108Application {

    public static void main(String[] args) {
        SpringApplication.run(Emergency108Application.class, args);
    }

}
