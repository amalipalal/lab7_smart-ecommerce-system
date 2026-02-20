package com.example.ecommerce_system.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.ecommerce_system.dto.auth.AuthResponseDto;
import com.example.ecommerce_system.dto.auth.LoginRequestDto;
import com.example.ecommerce_system.dto.auth.SignupRequestDto;
import com.example.ecommerce_system.exception.auth.DuplicateEmailException;
import com.example.ecommerce_system.exception.auth.InvalidCredentialsException;
import com.example.ecommerce_system.exception.auth.UserNotFoundException;
import com.example.ecommerce_system.exception.auth.WeakPasswordException;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.model.Role;
import com.example.ecommerce_system.model.RoleType;
import com.example.ecommerce_system.model.User;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.repository.RoleRepository;
import com.example.ecommerce_system.repository.UserRepository;
import com.example.ecommerce_system.util.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;

    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user with the provided credentials.
     * Validates password strength, checks for duplicate email, hashes the password, and persists the user.
     * Also creates a customer record for the new user.
     */
    @Transactional
    public AuthResponseDto signup(SignupRequestDto request) {
        validatePassword(request.getPassword());

        Optional<User> existingUser = userRepository.findUserByEmail(request.getEmail());
        if (existingUser.isPresent())
            throw new DuplicateEmailException(request.getEmail());

        var newUser = createUser(request);
        var newCustomer = createCustomer(request, newUser);

        var createdUser = userRepository.save(newUser);
        customerRepository.save(newCustomer);
        return authMapper.toDTO(createdUser, null);
    }

    private User createUser(SignupRequestDto request) {
        Role customerRole = roleRepository.findRoleByRoleName(RoleType.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Customer role not found"));

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        return User.builder()
                .userId(UUID.randomUUID())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(customerRole)
                .createdAt(Instant.now())
                .build();
    }

    private Customer createCustomer(SignupRequestDto request, User user) {
        return Customer.builder()
                .customerId(UUID.randomUUID())
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .active(true)
                .build();
    }

    /**
     * Authenticate a user with email and password.
     * Verifies credentials and returns user details if valid.
     */
    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findUserByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new InvalidCredentialsException();

        String token = generateJwtToken(user);
        return authMapper.toDTO(user, token);
    }

    private String generateJwtToken(User user) {
        return jwtTokenService.generateToken(user);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters long.");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter.");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter.");
        }

        if (!password.matches(".*\\d.*")) {
            throw new WeakPasswordException("Password must contain at least one digit.");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new WeakPasswordException("Password must contain at least one special character.");
        }
    }
}
