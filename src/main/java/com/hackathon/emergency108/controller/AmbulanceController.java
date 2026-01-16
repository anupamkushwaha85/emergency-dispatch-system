package com.hackathon.emergency108.controller;

import com.hackathon.emergency108.auth.guard.AuthGuard;
import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ambulances")
public class AmbulanceController {

    private final AmbulanceRepository ambulanceRepository;
    private final AuthGuard authGuard;

    public AmbulanceController(AmbulanceRepository ambulanceRepository,
                               AuthGuard authGuard) {
        this.ambulanceRepository = ambulanceRepository;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }
}
