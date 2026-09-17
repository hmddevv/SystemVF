package com.hmdao.farm.identity.infrastructure;

import com.hmdao.farm.identity.application.port.UserRepository;
import com.hmdao.farm.identity.domain.User;
import org.springframework.data.repository.Repository;

interface SpringDataUserRepository extends Repository<User, Long>, UserRepository {
}
