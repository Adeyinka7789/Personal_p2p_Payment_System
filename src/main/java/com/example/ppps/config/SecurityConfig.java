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
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                )
                .authorizeHttpRequests(requests -> requests
                        // Public API Endpoints
                        .requestMatchers("/api/v1/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()

                        // Static resources for admin UI
                        .requestMatchers("/admin/**", "/static/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/login", "/error", "/favicon.ico").permitAll()

                        // Swagger/OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // Secured API Endpoints - Require JWT
                        .requestMatchers(
                                "/api/v1/funding",
                                "/api/v1/withdrawals",
                                "/api/v1/transfers",
                                "/api/v1/balance/**",
                                "/api/v1/transactions/**",
                                "/api/v1/reset-pin/**"
                        ).authenticated()

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                // Form login for web UI
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin/index.html", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Session management: stateless for API, stateful for web UI
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                // Filters for API endpoints
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, RateLimitingFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}