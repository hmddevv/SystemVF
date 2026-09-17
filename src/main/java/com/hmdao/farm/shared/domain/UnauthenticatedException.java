package com.hmdao.farm.shared.domain;

/**
 * Không xác định được người dùng hiện tại.
 */
public class UnauthenticatedException extends DomainException {

    public UnauthenticatedException(String message) {
        super("AUTH", message);
    }
}
