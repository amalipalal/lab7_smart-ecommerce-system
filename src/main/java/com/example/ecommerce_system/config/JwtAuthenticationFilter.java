package com.example.ecommerce_system.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.ecommerce_system.service.JwtTokenService;
import com.example.ecommerce_system.service.TokenBlacklistService;
import com.example.ecommerce_system.model.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null) {
                DecodedJWT decodedJWT = jwtTokenService.validateToken(token);
                String jti = jwtTokenService.extractJti(decodedJWT);

                if (!tokenBlacklistService.isBlacklisted(jti)) {
                    setAuthentication(decodedJWT);
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (JWTVerificationException e) {
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.debug("Authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void setAuthentication(DecodedJWT decodedJWT) {
        String userId = jwtTokenService.extractUserId(decodedJWT);
        String email = jwtTokenService.extractEmail(decodedJWT);
        String roleWithPrefix = jwtTokenService.extractRoleWithPrefix(decodedJWT);

        CustomUserDetails userDetails = buildUserDetails(userId, email, roleWithPrefix);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private CustomUserDetails buildUserDetails(String userId, String email, String roleWithPrefix) {
        return CustomUserDetails.builder()
                .userId(UUID.fromString(userId))
                .email(email)
                .authorities(Collections.singleton(new SimpleGrantedAuthority(roleWithPrefix)))
                .build();
    }
}
