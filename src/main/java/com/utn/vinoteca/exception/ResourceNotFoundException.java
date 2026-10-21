package com.utn.vinoteca.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message, UUID id) {
        super(message + id);;
    }
}
