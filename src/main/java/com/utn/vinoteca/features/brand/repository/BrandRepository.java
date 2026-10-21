package com.utn.vinoteca.features.brand.repository;

import com.utn.vinoteca.features.brand.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

    Optional<BrandEntity> findByExternalId(UUID externalId);
    boolean existsByName(String name);

    // Nuevos metodos para consistencia con el campo 'active'
    Optional<BrandEntity> findByExternalIdAndActiveTrue(UUID externalId);
    boolean existsByNameAndActiveTrue(String name);
    List<BrandEntity> findAllByActiveTrue(); // Para getAll() por defecto
    List<BrandEntity> findAllByActiveFalse(); // Para un posible getInactive()
}