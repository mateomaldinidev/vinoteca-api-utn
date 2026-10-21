package com.utn.vinoteca.features.winery.repository;

import com.utn.vinoteca.features.winery.entity.WineryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WineryRepository extends JpaRepository<WineryEntity, Long> {

    Optional<WineryEntity> findByExternalId(UUID externalId);
    boolean existsByName(String name);

    // Nuevos metodos para consistencia con el campo 'active'
    Optional<WineryEntity> findByExternalIdAndActiveTrue(UUID externalId);
    boolean existsByNameAndActiveTrue(String name);
    List<WineryEntity> findAllByActiveTrue(); // Para getAll() por defecto
    List<WineryEntity> findAllByActiveFalse(); // Para un posible getInactive()
}