package com.example.ppps.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                if (claims.getExpiration().before(new Date())) {
                    throw new SecurityException("Token has expired");
                }

                String userId = claims.getSubject();

                // ✅ FIX: Extract role from JWT claims
                String role = claims.get("role", String.class);
                System.out.println("🔐 JWT Role extracted: " + role + " for user: " + userId);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                if (role != null) {
                    // Add ROLE_ prefix for Spring Security
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                } else {
                    // Default to USER if no role found
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    System.out.println("⚠️ No role found in JWT, defaulting to ROLE_USER");
                }

                // ✅ Use custom JwtAuthenticationToken
                JwtAuthenticationToken authentication =
                        new JwtAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                System.err.println("❌ JWT Validation Failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}