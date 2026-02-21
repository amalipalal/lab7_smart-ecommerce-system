package com.example.ecommerce_system.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.ecommerce_system.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class JwtTokenService {

    @Value("${jwt.token.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expiration-ms:86400000}")
    private long expirationMs;

    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withSubject(user.getUserId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().getRoleName().name())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.require(algorithm).build().verify(token);
    }

    public String extractUserId(DecodedJWT decodedJWT) {
        return decodedJWT.getSubject();
    }

    public String extractRole(DecodedJWT decodedJWT) {
        return decodedJWT.getClaim("role").asString();
    }

    public String extractEmail(DecodedJWT decodedJWT) {
        return decodedJWT.getClaim("email").asString();
    }

    public String extractRoleWithPrefix(DecodedJWT decodedJWT) {
        return "ROLE_" + extractRole(decodedJWT);
    }

    public String extractJti(DecodedJWT decodedJWT) {
        return decodedJWT.getId();
    }
}
