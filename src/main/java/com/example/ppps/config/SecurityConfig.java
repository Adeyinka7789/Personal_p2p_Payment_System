package com.example.ppps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;


@Configuration
@EnableWebSecurity
@EnableCaching
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RateLimitingFilter rateLimitingFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // i have enabled CORS with Spring Security configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                )
                .authorizeHttpRequests(requests -> requests
                        // Public Pages & Static Assets
                        .requestMatchers("/", "/login", "/register").permitAll()
                        // kindly Allow all (CSS, JS, images, etc.)
                        .requestMatchers(
                                "/users/**",
                                "/admin/**",  // This covers all admin pages including login.html
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/favicon.ico"
                        ).permitAll()
                        // kindly allow all public API endpoints
                        .requestMatchers(
                                "/api/v1/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/admin/login",
                                "/api/v1/webhooks/**"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/error",
                                "/actuator/**"
                        ).permitAll()
                        // kindly permit all dashboard pages-- for the JWT handled client-side
                        .requestMatchers("/dashboard", "/dashboard/**").permitAll()
                        .requestMatchers("/admin/dashboard", "/admin/dashboard/**").permitAll()
                        // for secured Endpoints --- requires JWT
                        .requestMatchers(
                                "/api/v1/funding",
                                "/api/v1/withdrawals",
                                "/api/v1/transfers",
                                "/api/v1/balance/**",
                                "/api/v1/transactions/**",
                                "/api/v1/reset-pin/**",
                                "/api/v1/user-info"
                        ).authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // every other request must be authenticated
                        .anyRequest().authenticated()
                )
                // i have disabled form login, use JWT
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // for my Session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Changed to STATELESS for pure JWT
                )
                // Filter chain: Rate Limiting --> JWT Authentication --> Spring Security
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // kindly allow some origins with patterns
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.ngrok-free.app",
                "https://ef957486f0d1.ngrok-free.app"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // cookies & authorization headers
        configuration.setAllowCredentials(true);
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        // Expose headers to the client
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}