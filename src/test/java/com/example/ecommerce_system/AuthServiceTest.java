package com.example.ecommerce_system;

import com.example.ecommerce_system.dto.auth.AuthResponseDto;
import com.example.ecommerce_system.dto.auth.LoginRequestDto;
import com.example.ecommerce_system.dto.auth.SignupRequestDto;
import com.example.ecommerce_system.exception.auth.DuplicateEmailException;
import com.example.ecommerce_system.exception.auth.InvalidCredentialsException;
import com.example.ecommerce_system.exception.auth.WeakPasswordException;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.model.Role;
import com.example.ecommerce_system.model.RoleType;
import com.example.ecommerce_system.model.User;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.repository.RoleRepository;
import com.example.ecommerce_system.repository.UserRepository;
import com.example.ecommerce_system.service.AuthService;
import com.example.ecommerce_system.service.JwtTokenService;
import com.example.ecommerce_system.service.TokenBlacklistService;
import com.example.ecommerce_system.util.mapper.AuthMapper;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should signup user successfully")
    void shouldSignupUserSuccessfully() {
        SignupRequestDto request = new SignupRequestDto(
                "admin@example.com",
                "Password123!",
                "John",
                "Doe",
                "+233123456789"
        );

        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("admin@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(Instant.now())
                .build();

        Customer savedCustomer = Customer.builder()
                .customerId(UUID.randomUUID())
                .user(savedUser)
                .firstName("John")
                .lastName("Doe")
                .phone("+233123456789")
                .active(true)
                .build();

        AuthResponseDto authResponse = AuthResponseDto.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .roleName(RoleType.CUSTOMER)
                .createdAt(savedUser.getCreatedAt())
                .build();

        when(userRepository.findUserByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByRoleName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(authMapper.toDTO(savedUser, null)).thenReturn(authResponse);

        AuthResponseDto response = authService.signup(request);

        Assertions.assertEquals("admin@example.com", response.getEmail());
        Assertions.assertEquals(RoleType.CUSTOMER, response.getRoleName());
        verify(userRepository).findUserByEmail("admin@example.com");
        verify(roleRepository).findRoleByRoleName(RoleType.CUSTOMER);
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
        verify(authMapper).toDTO(savedUser, null);
    }

    @Test
    @DisplayName("Should throw error when signing up with duplicate email")
    void shouldThrowWhenSigningUpWithDuplicateEmail() {
        SignupRequestDto request = new SignupRequestDto(
                "existing@example.com",
                "Password123!",
                "Alice",
                "Brown",
                "+233111222333"
        );

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email("existing@example.com")
                .passwordHash("hashedPassword")
                .role(Role.builder().roleName(RoleType.CUSTOMER).build())
                .createdAt(Instant.now())
                .build();

        when(userRepository.findUserByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        Assertions.assertThrows(
                DuplicateEmailException.class,
                () -> authService.signup(request)
        );

        verify(userRepository).findUserByEmail("existing@example.com");
        verify(roleRepository, never()).findRoleByRoleName(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when password is too short")
    void shouldThrowWhenPasswordIsTooShort() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "Pass1!",
                "Tom",
                "White",
                "+233444555666"
        );

        Assertions.assertThrows(
                WeakPasswordException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).findUserByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when password has no uppercase letter")
    void shouldThrowWhenPasswordHasNoUppercaseLetter() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "password123!",
                "Sam",
                "Green",
                "+233777888999"
        );

        Assertions.assertThrows(
                WeakPasswordException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).findUserByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when password has no lowercase letter")
    void shouldThrowWhenPasswordHasNoLowercaseLetter() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "PASSWORD123!",
                "Mike",
                "Black",
                "+233555666777"
        );

        Assertions.assertThrows(
                WeakPasswordException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).findUserByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when password has no digit")
    void shouldThrowWhenPasswordHasNoDigit() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "Password!",
                "Chris",
                "Gray",
                "+233888999000"
        );

        Assertions.assertThrows(
                WeakPasswordException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).findUserByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when password has no special character")
    void shouldThrowWhenPasswordHasNoSpecialCharacter() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "Password123",
                "David",
                "Blue",
                "+233123123123"
        );

        Assertions.assertThrows(
                WeakPasswordException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).findUserByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when password is null")
    void shouldThrowWhenPasswordIsNull() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                null,
                "Emma",
                "Red",
                "+233456456456"
        );

        Assertions.assertThrows(
                WeakPasswordException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).findUserByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() {
        LoginRequestDto request = new LoginRequestDto(
                "user@example.com",
                "Password123!"
        );

        User user = User.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(Role.builder().roleName(RoleType.CUSTOMER).build())
                .createdAt(Instant.now())
                .build();

        AuthResponseDto authResponse = AuthResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .roleName(RoleType.CUSTOMER)
                .createdAt(user.getCreatedAt())
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateToken(user)).thenReturn("jwt-token-string");
        when(authMapper.toDTO(eq(user), anyString())).thenReturn(authResponse);

        AuthResponseDto response = authService.login(request);

        Assertions.assertEquals("user@example.com", response.getEmail());
        Assertions.assertEquals(RoleType.CUSTOMER, response.getRoleName());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findUserByEmail("user@example.com");
        verify(jwtTokenService).generateToken(user);
        verify(authMapper).toDTO(eq(user), eq("jwt-token-string"));
    }

    @Test
    @DisplayName("Should throw error when login with non-existing email")
    void shouldThrowWhenLoginWithNonExistingEmail() {
        LoginRequestDto request = new LoginRequestDto(
                "nonexisting@example.com",
                "Password123!"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findUserByEmail(any());
    }

    @Test
    @DisplayName("Should throw error when login with incorrect password")
    void shouldThrowWhenLoginWithIncorrectPassword() {
        LoginRequestDto request = new LoginRequestDto(
                "user@example.com",
                "WrongPassword123!"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findUserByEmail(any());
    }

    @Test
    @DisplayName("Should hash password when creating user")
    void shouldHashPasswordWhenCreatingUser() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "Password123!",
                "Lisa",
                "Purple",
                "+233789789789"
        );

        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(Instant.now())
                .build();

        Customer savedCustomer = Customer.builder()
                .customerId(UUID.randomUUID())
                .user(savedUser)
                .firstName("Lisa")
                .lastName("Purple")
                .phone("+233789789789")
                .active(true)
                .build();

        when(userRepository.findUserByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByRoleName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(authMapper.toDTO(any(User.class), isNull())).thenReturn(mock(AuthResponseDto.class));

        authService.signup(request);

        verify(passwordEncoder).encode("Password123!");
        verify(roleRepository).findRoleByRoleName(RoleType.CUSTOMER);
        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("hashedPassword")
        ));
    }

    @Test
    @DisplayName("Should return user details in auth response for signup")
    void shouldReturnUserDetailsInAuthResponseForSignup() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "Password123!",
                "Mark",
                "Orange",
                "+233321321321"
        );

        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .userId(userId)
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(createdAt)
                .build();

        Customer savedCustomer = Customer.builder()
                .customerId(UUID.randomUUID())
                .user(savedUser)
                .firstName("Mark")
                .lastName("Orange")
                .phone("+233321321321")
                .active(true)
                .build();

        AuthResponseDto authResponse = AuthResponseDto.builder()
                .userId(userId)
                .email("user@example.com")
                .roleName(RoleType.CUSTOMER)
                .createdAt(createdAt)
                .build();

        when(userRepository.findUserByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByRoleName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(authMapper.toDTO(savedUser, null)).thenReturn(authResponse);

        AuthResponseDto response = authService.signup(request);

        Assertions.assertEquals(userId, response.getUserId());
        Assertions.assertEquals("user@example.com", response.getEmail());
        Assertions.assertEquals(RoleType.CUSTOMER, response.getRoleName());
        Assertions.assertEquals(createdAt, response.getCreatedAt());
        verify(roleRepository).findRoleByRoleName(RoleType.CUSTOMER);
    }

    @Test
    @DisplayName("Should return user details in auth response for login")
    void shouldReturnUserDetailsInAuthResponseForLogin() {
        LoginRequestDto request = new LoginRequestDto(
                "user@example.com",
                "Password123!"
        );

        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User user = User.builder()
                .userId(userId)
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(createdAt)
                .build();

        AuthResponseDto authResponse = AuthResponseDto.builder()
                .userId(userId)
                .email("user@example.com")
                .roleName(RoleType.CUSTOMER)
                .createdAt(createdAt)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateToken(user)).thenReturn("jwt-token-string");
        when(authMapper.toDTO(eq(user), anyString())).thenReturn(authResponse);

        AuthResponseDto response = authService.login(request);

        Assertions.assertEquals(userId, response.getUserId());
        Assertions.assertEquals("user@example.com", response.getEmail());
        Assertions.assertEquals(RoleType.CUSTOMER, response.getRoleName());
        Assertions.assertEquals(createdAt, response.getCreatedAt());
        verify(jwtTokenService).generateToken(user);
    }

    @Test
    @DisplayName("Should accept password with all required character types")
    void shouldAcceptPasswordWithAllRequiredCharacterTypes() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "ValidPass123!",
                "Nina",
                "Yellow",
                "+233654654654"
        );

        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(Instant.now())
                .build();

        Customer savedCustomer = Customer.builder()
                .customerId(UUID.randomUUID())
                .user(savedUser)
                .firstName("Nina")
                .lastName("Yellow")
                .phone("+233654654654")
                .active(true)
                .build();

        when(userRepository.findUserByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByRoleName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(authMapper.toDTO(any(User.class), isNull())).thenReturn(mock(AuthResponseDto.class));

        Assertions.assertDoesNotThrow(() -> authService.signup(request));

        verify(userRepository).save(any(User.class));
        verify(roleRepository).findRoleByRoleName(RoleType.CUSTOMER);
    }

    @Test
    @DisplayName("Should accept password with different special characters")
    void shouldAcceptPasswordWithDifferentSpecialCharacters() {
        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(Instant.now())
                .build();

        Customer savedCustomer = Customer.builder()
                .customerId(UUID.randomUUID())
                .user(savedUser)
                .firstName("Test")
                .lastName("User")
                .phone("+233000000000")
                .active(true)
                .build();

        SignupRequestDto request1 = new SignupRequestDto("user1@example.com", "Password123@", "Paul", "Pink", "+233987987987");
        SignupRequestDto request2 = new SignupRequestDto("user2@example.com", "Password123#", "Rachel", "Brown", "+233147147147");
        SignupRequestDto request3 = new SignupRequestDto("user3@example.com", "Password123$", "Steve", "Cyan", "+233258258258");

        when(userRepository.findUserByEmail(any())).thenReturn(Optional.empty());
        when(roleRepository.findRoleByRoleName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(authMapper.toDTO(any(User.class), isNull())).thenReturn(mock(AuthResponseDto.class));

        Assertions.assertDoesNotThrow(() -> authService.signup(request1));
        Assertions.assertDoesNotThrow(() -> authService.signup(request2));
        Assertions.assertDoesNotThrow(() -> authService.signup(request3));

        verify(roleRepository, times(3)).findRoleByRoleName(RoleType.CUSTOMER);
    }

    @Test
    @DisplayName("Should generate unique user IDs for different signups")
    void shouldGenerateUniqueUserIdsForDifferentSignups() {
        SignupRequestDto request = new SignupRequestDto(
                "user@example.com",
                "Password123!",
                "Victor",
                "Magenta",
                "+233369369369"
        );

        Role customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleType.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(customerRole)
                .createdAt(Instant.now())
                .build();

        Customer savedCustomer = Customer.builder()
                .customerId(UUID.randomUUID())
                .user(savedUser)
                .firstName("Victor")
                .lastName("Magenta")
                .phone("+233369369369")
                .active(true)
                .build();

        when(userRepository.findUserByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByRoleName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(authMapper.toDTO(any(User.class), isNull())).thenReturn(mock(AuthResponseDto.class));

        authService.signup(request);

        verify(userRepository).save(argThat(user -> user.getUserId() != null));
        verify(roleRepository).findRoleByRoleName(RoleType.CUSTOMER);
    }

    @Test
    @DisplayName("Should logout user successfully")
    void shouldLogoutUserSuccessfully() {
        String token = "valid-jwt-token";
        DecodedJWT decodedJWT = mock(DecodedJWT.class);

        when(jwtTokenService.validateToken(token)).thenReturn(decodedJWT);
        when(jwtTokenService.extractJti(decodedJWT)).thenReturn("token-jti-123");

        authService.logout(token);

        verify(jwtTokenService).validateToken(token);
        verify(jwtTokenService).extractJti(decodedJWT);
        verify(tokenBlacklistService).blacklistToken("token-jti-123");
    }

    @Test
    @DisplayName("Should handle exception when logout with invalid token")
    void shouldHandleExceptionWhenLogoutWithInvalidToken() {
        String token = "invalid-jwt-token";

        when(jwtTokenService.validateToken(token)).thenThrow(new RuntimeException("Invalid token"));

        authService.logout(token);

        verify(jwtTokenService).validateToken(token);
        verify(tokenBlacklistService, never()).blacklistToken(any());
    }

    @Test
    @DisplayName("Should blacklist token with correct JTI")
    void shouldBlacklistTokenWithCorrectJti() {
        String token = "jwt-token-string";
        String jti = "unique-jti-value";
        DecodedJWT decodedJWT = mock(DecodedJWT.class);

        when(jwtTokenService.validateToken(token)).thenReturn(decodedJWT);
        when(jwtTokenService.extractJti(decodedJWT)).thenReturn(jti);

        authService.logout(token);

        verify(tokenBlacklistService).blacklistToken(jti);
    }

    @Test
    @DisplayName("Should not throw exception on logout error")
    void shouldNotThrowExceptionOnLogoutError() {
        String token = "malformed-token";

        when(jwtTokenService.validateToken(token)).thenThrow(new IllegalArgumentException("Malformed JWT"));

        Assertions.assertDoesNotThrow(() -> authService.logout(token));

        verify(jwtTokenService).validateToken(token);
    }
}
