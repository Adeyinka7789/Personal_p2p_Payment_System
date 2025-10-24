package com.example.ppps.config;

import com.example.ppps.config.JwtAuthenticationFilter;
import com.example.ppps.config.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
@EnableCaching
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RateLimitingFilter rateLimitingFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // Public Endpoint 1: Registration (FR1.1)
                        .requestMatchers("/api/v1/register").permitAll()

                        // Public Endpoint 2: The standard Login path
                        .requestMatchers("/api/v1/auth/login").permitAll()

                        // Secured Endpoints - Require a valid JWT for all financial operations
                        .requestMatchers("/api/v1/funding",         // Funding (Deposit)
                                "/api/v1/transfers",                 // P2P Transfer (FR1.3)
                                "/api/v1/balance/**",                // Balance Inquiry (FR1.5)
                                "/api/v1/transactions/**",           // Transaction History (FR1.4)
                                "/api/v1/reset-pin/**").authenticated() // PIN Reset/Change

                        // Fallback: Any other request must also be authenticated
                        .anyRequest().authenticated()
                )
                // The Rate Limiting Filter runs first to protect against floods on any endpoint
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                // The JWT Filter runs second to establish the user's identity
                .addFilterBefore(jwtAuthenticationFilter, RateLimitingFilter.class);


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
