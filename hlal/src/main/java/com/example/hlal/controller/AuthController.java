package com.example.hlal.controller;

import com.example.hlal.dto.request.LoginRequest;
import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.dto.response.LoginResponse;
import com.example.hlal.dto.response.RegisterResponse;
import com.example.hlal.model.Users;
import com.example.hlal.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.text.html.Option;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private UsersService usersService;

    @PostMapping("/api/auth/register")
    public ResponseEntity<RegisterResponse>register(@RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse =new RegisterResponse();
        try {
            Users register = usersService.register(registerRequest);
            registerResponse.setStatus("Success");
            registerResponse.setMessage("Berhasil Registrasi");
        }catch (Exception e){
            registerResponse.setStatus("Error");
            registerResponse.setMessage(e.getMessage());
        }
        return ResponseEntity.ok(registerResponse);
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = new LoginResponse();;
        try {
            // 🔥 Panggil service login
            String token = usersService.login(loginRequest);


            loginResponse.setStatus("Success");
            loginResponse.setMessage("Berhasil Login");
            loginResponse.setToken(token); // kalau kamu punya field `token` di LoginResponse
            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            loginResponse.setStatus("Error");
            loginResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
        }
    }
}
