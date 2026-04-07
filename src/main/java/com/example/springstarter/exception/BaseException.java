package com.example.springstarter.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final int status;

    public BaseException(String message, int status) {
        super(message);
        this.status = status;
    }
}
