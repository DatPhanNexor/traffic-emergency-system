package com.example.suco.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.suco.model.User;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);

    List<User> findAllByProviderNot(String provider);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndProviderNot(
            String name,
            String email,
            String provider
    );
}