package com.example.ecommerce_system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessResponseDto<T>(HttpStatus status, String message, T data){}
