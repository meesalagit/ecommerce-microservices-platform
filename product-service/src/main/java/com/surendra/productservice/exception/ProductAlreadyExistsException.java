package com.surendra.productservice.exception;

public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}