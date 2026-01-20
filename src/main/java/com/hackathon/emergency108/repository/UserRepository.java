package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.DriverVerificationStatus;
import com.hackathon.emergency108.entity.User;
import com.hackathon.emergency108.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);
    
    List<User> findByRoleAndDriverVerificationStatus(UserRole role, DriverVerificationStatus status);
}

