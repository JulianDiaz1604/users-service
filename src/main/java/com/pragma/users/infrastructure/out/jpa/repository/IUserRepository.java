package com.pragma.users.infrastructure.out.jpa.repository;

import com.pragma.users.infrastructure.out.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);
}
