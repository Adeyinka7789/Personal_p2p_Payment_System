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
                // Disable CORS (handled by WebConfig) and CSRF protection explicitly for API endpoints
                .cors(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                )
                .authorizeHttpRequests(requests -> requests
                        // Public Pages & Static Assets
                        .requestMatchers("/", "/login", "/register").permitAll()
                        .requestMatchers("/admin/login").permitAll()

                        // Static resources - Allow all (CSS, JS, images, etc.)
                        .requestMatchers(
                                "/users/**",
                                "/admin/**",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Public API Endpoints
                        .requestMatchers("/api/v1/register", "/api/v1/auth/login").permitAll()

                        // Other public endpoints/assets
                        .requestMatchers("/error", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()

                        // User dashboard: MUST BE PERMIT ALL! The JWT check is done in the browser's JavaScript.
                        .requestMatchers("/dashboard").permitAll()

                        // Admin dashboard: This is typically protected via session/cookie in a traditional setup,
                        // but since we are using JWT, we allow all here and protect it client-side as well.
                        // NOTE: If you plan to protect the admin panel with a server-side session, this needs adjustment.
                        .requestMatchers("/admin/dashboard").permitAll()

                        // Secured API Endpoints - Require JWT
                        .requestMatchers(
                                "/api/v1/funding",
                                "/api/v1/withdrawals",
                                "/api/v1/transfers",
                                "/api/v1/balance/**",
                                "/api/v1/transactions/**",
                                "/api/v1/reset-pin/**",
                                "/api/v1/user-info" // Assuming you'll add an endpoint to fetch user details
                        ).authenticated()

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )

                // IMPORTANT: Since you are using a stateless JWT setup with AJAX login,
                // we explicitly disable the formLogin() and logout() built-in features.
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // Session management policy: Stateless for API access, but IF_REQUIRED
                // allows Spring to handle requests if needed (e.g., /dashboard).
                // We keep IF_REQUIRED for now to maintain flexibility, but pure JWT apps often use STATELESS.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // Filter chain: Rate Limiting → JWT Authentication → Spring Security
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}