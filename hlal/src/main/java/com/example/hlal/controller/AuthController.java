package com.example.hlal.controller;

import com.example.hlal.dto.request.EditProfileRequest;
import com.example.hlal.dto.request.LoginRequest;
import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.dto.response.EditProfileResponse;
import com.example.hlal.dto.response.LoginResponse;
import com.example.hlal.dto.response.RegisterResponse;
import com.example.hlal.model.Users;
import com.example.hlal.service.JWTService;
import com.example.hlal.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private UsersService usersService;

    @Autowired
    private JWTService jwtService;

//    @PostMapping("/api/auth/register")
//    public ResponseEntity<RegisterResponse>register(@RequestBody RegisterRequest registerRequest) {
//        RegisterResponse registerResponse =new RegisterResponse();
//        try {
//            Users register = usersService.register(registerRequest);
//            registerResponse.setStatus("Success");
//            registerResponse.setMessage("Berhasil Registrasi");
//        }catch (Exception e){
//            registerResponse.setStatus("Error");
//            registerResponse.setMessage(e.getMessage());
//        }
//        return ResponseEntity.ok(registerResponse);
//    }

    @PostMapping(value = "/api/auth/register", consumes = {"multipart/form-data"})
    public ResponseEntity<RegisterResponse> register(@ModelAttribute RegisterRequest registerRequest) {
        RegisterResponse response = new RegisterResponse();
        try {
            Users registeredUser = usersService.register(registerRequest);
            response.setStatus("Success");
            response.setMessage("Berhasil Registrasi");
            response.setEmail(registeredUser.getEmail());
            response.setFullname(registeredUser.getFullname());
            response.setUsername(registerRequest.getUsername());
            response.setPhoneNumber(registeredUser.getPhoneNumber());
            response.setAvatarUrl(registeredUser.getAvatarUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatus("Error");
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = new LoginResponse();;
        try {
            // Panggil service login
            String token = usersService.login(loginRequest);
            loginResponse.setStatus("Success");
            loginResponse.setMessage("Berhasil Login");
            loginResponse.setToken(token);
            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            loginResponse.setStatus("Error");
            loginResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
        }
    }

    @PostMapping(value = "/api/auth/edit-profile", consumes = {"multipart/form-data"})
    public ResponseEntity<EditProfileResponse> editProfile(
            @ModelAttribute EditProfileRequest request,
            @RequestHeader("Authorization") String token
    ) {
        EditProfileResponse response = new EditProfileResponse();
        try {
            String email = jwtService.extractUsername(token.substring(7)); // Hapus "Bearer "
            EditProfileResponse result = usersService.editProfile(email, request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            response.setStatus("Error");
            response.setMessage(e.getMessage());
            response.setAvatarUrl(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}
