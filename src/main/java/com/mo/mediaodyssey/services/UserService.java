package com.mo.mediaodyssey.services;

import com.mo.mediaodyssey.models.User;
import com.mo.mediaodyssey.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User registerUser(String username, String email, String password){

        if(userRepository.existsByUsername(username)){
            throw new IllegalStateException("Username already exists");
        }

        User user = new User(username,email,password);

        return userRepository.save(user);
    }

    public User loginUser(String username, String password){

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Invalid username or password"));

        if(!user.getPassword().equals(password)){
            throw new IllegalStateException("Invalid username or password");
        }

        return user;
    }

    public String getUsernameById(Integer id){

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found"))
                .getUsername();
    }

}