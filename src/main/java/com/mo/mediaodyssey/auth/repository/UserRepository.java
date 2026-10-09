package com.mo.mediaodyssey.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mo.mediaodyssey.shared.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Repository containing the User model for authentication.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}