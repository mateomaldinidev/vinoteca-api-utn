package com.utn.vinoteca.security.repository;

import com.utn.vinoteca.security.entity.RoleEntity;
import com.utn.vinoteca.security.enums.Roles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolesRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRole(Roles role);

}
