package com.example.springstarter.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    
    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST.value()); // Default 400 for business logic errors
    }

    public BusinessException(String message, int status) {
        super(message, status);
    }
}
