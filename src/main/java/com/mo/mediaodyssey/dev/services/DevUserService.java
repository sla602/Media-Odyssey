package com.mo.mediaodyssey.dev.services;

import com.mo.mediaodyssey.auth.dto.UserDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.layout.services.AvatarService;
import com.mo.mediaodyssey.shared.model.User;
import java.util.UUID;

@Service
public class DevUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void developmentalAccountCreation(UserDto dto, String role) {
        // Check if a User with this email address already exist.
        Boolean userExists = userRepository.existsByEmail(dto.email());

        if (userExists) {
            throw new BadCredentialsException("User with email address " + dto.email() + " already exists.");
        }

        // Create the User
        // new users will have a random generated avatars and their custom avatar will
        // be null until they upload something
        User user = new User(dto.email(), dto.email(), passwordEncoder.encode(dto.password()), true, true, true,
                role, "LOCAL", null, null,
                AvatarService.avatarGenerate(Math.abs(UUID.randomUUID().getMostSignificantBits())), null, "default");

        // Save the user
        userRepository.save(user);
    }

    @Transactional
    public void createAdminAccount(UserDto dto) {
        this.developmentalAccountCreation(dto, "ROLE_ADMIN");
    }

    @Transactional
    public void createUserAccount(UserDto dto) {
        this.developmentalAccountCreation(dto, "ROLE_USER");
    }
}
