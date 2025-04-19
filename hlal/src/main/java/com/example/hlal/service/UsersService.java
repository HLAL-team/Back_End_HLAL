package com.example.hlal.service;


import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.model.Users;
import com.example.hlal.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    public Users register(RegisterRequest registerRequest) {
        Optional<Users> checkUserByEmail = this.usersRepository.findByEmail(registerRequest.getEmail());

        if (checkUserByEmail.isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        Users user = new Users();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setFullname(registerRequest.getFullname());
        user.setPassword(registerRequest.getPassword());
        return this.usersRepository.save(user);
    }
}
