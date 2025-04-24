package com.example.hlal.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                ).csrf(csrf -> csrf.disable()).cors(cors->cors.configurationSource(request ->{
                    var corsConfig = new CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of(
                            "http://localhost:5173",
                            "http://localhost:3000"
                    ));
                    corsConfig.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("Authorization","Content-Type"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

