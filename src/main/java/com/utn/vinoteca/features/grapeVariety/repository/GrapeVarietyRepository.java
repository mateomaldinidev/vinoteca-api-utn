package com.utn.vinoteca.features.grapeVariety.repository;

import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrapeVarietyRepository extends JpaRepository<GrapeVarietyEntity, Long> {

    Optional<GrapeVarietyEntity> findByExternalId(UUID externalId);
    Optional<GrapeVarietyEntity> findByExternalIdAndActiveTrue(UUID externalId);
    Optional<GrapeVarietyEntity> findByName(String name);
    boolean existsByName(String name);
    List<GrapeVarietyEntity> findAllByActiveTrue();
    List<GrapeVarietyEntity> findAllByActiveFalse();
}
