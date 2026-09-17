package com.hmdao.farm.identity.application.port;

public interface UserRepository {

    boolean existsById(Long id);
}
