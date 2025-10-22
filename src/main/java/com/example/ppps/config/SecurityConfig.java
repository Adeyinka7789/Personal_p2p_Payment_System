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

@Configuration
@EnableWebSecurity
@EnableCaching
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RateLimitingFilter rateLimitingFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // NEW API - not deprecated
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/register").permitAll()
                        // NEW FIX: Allow access to the funding endpoint for deposits
                        .requestMatchers("/api/v1/funding/**").authenticated()
                        // FIX: Changed singular "/api/v1/transfer" to plural "/api/v1/transfers"
                        .requestMatchers("/api/v1/transfers", "/api/v1/balance/**",
                                "/api/v1/transactions/**", "/api/v1/reset-pin/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}