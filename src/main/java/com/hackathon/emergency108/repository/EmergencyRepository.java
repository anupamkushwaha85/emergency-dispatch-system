package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.Emergency;
import com.hackathon.emergency108.entity.EmergencyStatus;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface EmergencyRepository extends JpaRepository<Emergency, Long> {

    List<Emergency> findByStatus(EmergencyStatus status);

}

