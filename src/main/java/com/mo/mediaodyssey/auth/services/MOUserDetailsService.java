package com.mo.mediaodyssey.auth.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MOUserDetailsService {

    // Inspired by:
    // https://www.baeldung.com/role-and-privilege-for-spring-security-registration
    // Documentation reviewed:
    // https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/userdetails/UserDetailsService.html

    @Autowired
    private UserRepository userRepository;

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
