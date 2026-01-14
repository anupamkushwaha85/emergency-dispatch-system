package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
