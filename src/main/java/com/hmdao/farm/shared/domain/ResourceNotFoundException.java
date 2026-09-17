package com.hmdao.farm.shared.domain;

/**
 * Không tìm thấy tài nguyên — hoặc tài nguyên thuộc chủ sở hữu khác (BR-11), để không lộ
 * sự tồn tại của dữ liệu người khác.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Long id) {
        super("BR-11", "Không tìm thấy %s với id %d.".formatted(resource, id));
    }
}
