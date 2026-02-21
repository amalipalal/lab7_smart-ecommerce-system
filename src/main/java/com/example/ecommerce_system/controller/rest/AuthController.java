package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.SuccessResponseDto;
import com.example.ecommerce_system.dto.auth.AuthResponseDto;
import com.example.ecommerce_system.dto.auth.LoginRequestDto;
import com.example.ecommerce_system.dto.auth.SignupRequestDto;
import com.example.ecommerce_system.service.AuthService;
import com.example.ecommerce_system.util.handler.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid input or weak password"),
            @ApiResponse(responseCode = "409", description = "Conflict - Email already exists")
    })
    @PostMapping("/signup")
    public SuccessResponseDto<AuthResponseDto> signup(@RequestBody @Valid SignupRequestDto request) {
        AuthResponseDto response = authService.signup(request);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.CREATED, response);
    }

    @Operation(summary = "Authenticate a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/login")
    public SuccessResponseDto<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, response);
    }

    @Operation(summary = "Logout a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid token")
    })
    @PostMapping("/logout")
    public SuccessResponseDto<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, null);
    }
}
