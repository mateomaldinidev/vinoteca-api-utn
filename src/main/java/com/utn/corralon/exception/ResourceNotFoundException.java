package com.utn.corralon.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message, Object id) {
        super(message + id);;
    }
}
