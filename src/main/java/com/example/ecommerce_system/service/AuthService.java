package com.example.ecommerce_system.service;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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
    private final TokenBlacklistService tokenBlacklistService;

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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.debug("Auth Error message is: {}", e.getMessage());
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findUserByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        String token = generateJwtToken(user);
        return authMapper.toDTO(user, token);
    }

    private String generateJwtToken(User user) {
        return jwtTokenService.generateToken(user);
    }

    @Transactional
    public User oauthSignup(String email, String firstName, String lastName) {
        log.debug("ABOUT TO START SIGNING UP {}", email);
        return userRepository.findUserByEmail(email).orElseGet(() -> {
            Role customerRole = roleRepository.findRoleByRoleName(RoleType.CUSTOMER)
                    .orElseThrow(() -> new IllegalStateException("Customer role not found"));

            User newUser = User.builder()
                    .userId(UUID.randomUUID())
                    .email(email)
                    .passwordHash("")
                    .role(customerRole)
                    .createdAt(Instant.now())
                    .build();

            User savedUser = userRepository.save(newUser);
            customerRepository.save(Customer.builder()
                    .customerId(UUID.randomUUID())
                    .user(savedUser)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .phone("")
                    .active(true)
                    .build());

            return savedUser;
        });
    }

    /**
     * Validate a token and blacklist jti of this token
     * to confirm complete logout.
     */
    public void logout(String token) {
        try {
            var decodedJWT = jwtTokenService.validateToken(token);
            String jti = jwtTokenService.extractJti(decodedJWT);
            tokenBlacklistService.blacklistToken(jti);
        } catch (Exception e) {
            log.debug("Logout error: {}", e.getMessage());
        }
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
