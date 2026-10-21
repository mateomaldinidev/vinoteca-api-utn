package com.utn.vinoteca.features.user.repository;

import com.utn.vinoteca.features.auth.Roles;
import com.utn.vinoteca.features.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByExternalId(UUID externalId);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByExternalId(UUID externalId);
    List<UserEntity> findAllByActiveTrue();
}
