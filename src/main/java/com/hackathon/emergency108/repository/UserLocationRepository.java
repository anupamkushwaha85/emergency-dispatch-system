package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    Optional<UserLocation> findByUser_Id(Long userId);
}
