package com.utn.vinoteca.security.repository;

import com.utn.vinoteca.security.entity.PermitEntity;
import com.utn.vinoteca.security.enums.Permits;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermitRepository extends JpaRepository<PermitEntity, Long> {
    Optional<PermitEntity> findByPermit(Permits permit);
}
