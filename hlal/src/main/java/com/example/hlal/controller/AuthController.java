package com.example.hlal.controller;

import com.example.hlal.dto.request.EditProfileRequest;
import com.example.hlal.dto.request.LoginRequest;
import com.example.hlal.dto.request.RegisterRequest;
import com.example.hlal.dto.response.EditProfileResponse;
import com.example.hlal.dto.response.LoginResponse;
import com.example.hlal.dto.response.RegisterResponse;
import com.example.hlal.dto.response.UserProfileResponse;
import com.example.hlal.model.Users;
import com.example.hlal.service.JWTService;
import com.example.hlal.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private UsersService usersService;

    @Autowired
    private JWTService jwtService;

    @PostMapping(value = "/api/auth/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        try {
            RegisterResponse response = usersService.register(registerRequest);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setStatus("Error");
            errorResponse.setMessage(e.getReason());
            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setStatus("Error");
            errorResponse.setMessage("Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = usersService.login(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setStatus("Error");
            loginResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
        }
    }

    @GetMapping("/api/auth/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UserProfileResponse profile = usersService.getProfile(token);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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

        } catch (RuntimeException e) {
            // Handle different exceptions by checking the message
            response.setStatus("Error");
            response.setMessage(e.getMessage());
            response.setUsername(null);
            response.setAvatarUrl(null);

            // Set status code based on error type
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else if (e.getMessage().contains("Username is already taken")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else if (e.getMessage().contains("Password must be at least 8 characters")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            response.setStatus("Error");
            response.setMessage("Profile update failed: " + e.getMessage());
            response.setUsername(null);
            response.setAvatarUrl(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}