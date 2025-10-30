package com.example.ppps.config;

import com.example.ppps.config.JwtAuthenticationFilter;
import com.example.ppps.config.RateLimitingFilter;
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
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableCaching
@EnableMethodSecurity(prePostEnabled = true) // ✅ ADD THIS LINE - CRITICAL FIX
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RateLimitingFilter rateLimitingFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // ENABLE CORS with Spring Security configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                )
                .authorizeHttpRequests(requests -> requests
                        // Public Pages & Static Assets
                        .requestMatchers("/", "/login", "/register").permitAll()

                        // Static resources - Allow all (CSS, JS, images, etc.)
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

                        // Public API Endpoints - COMBINED AND CLEANED
                        .requestMatchers(
                                "/api/v1/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/admin/login",  // ADDED HERE
                                "/api/v1/webhooks/**"
                        ).permitAll()

                        // Other public endpoints/assets
                        .requestMatchers(
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // Dashboard pages - permit all (JWT handled client-side)
                        .requestMatchers("/dashboard", "/dashboard/**").permitAll()
                        .requestMatchers("/admin/dashboard", "/admin/dashboard/**").permitAll()

                        // Secured API Endpoints - Require JWT
                        .requestMatchers(
                                "/api/v1/funding",
                                "/api/v1/withdrawals",
                                "/api/v1/transfers",
                                "/api/v1/balance/**",
                                "/api/v1/transactions/**",
                                "/api/v1/reset-pin/**",
                                "/api/v1/user-info"
                        ).authenticated()

                        // ✅ FIXED: Use hasRole instead of hasAuthority (automatically adds ROLE_ prefix)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )

                // Disable form login and use JWT
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // Session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Changed to STATELESS for pure JWT
                )

                // Filter chain: Rate Limiting → JWT Authentication → Spring Security
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins with patterns
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.ngrok-free.app",
                "https://ef957486f0d1.ngrok-free.app"
        ));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, authorization headers)
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