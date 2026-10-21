package com.utn.vinoteca.features.wineType.repository;

import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WineTypeRepository extends JpaRepository<WineTypeEntity, Long> {

    Optional<WineTypeEntity> findByExternalId(UUID externalId);
    boolean existsByName(String name); // Añadir este metodo para validacion de nombre

    // Nuevos metodos para consistencia con el campo 'active'
    Optional<WineTypeEntity> findByExternalIdAndActiveTrue(UUID externalId);
    boolean existsByNameAndActiveTrue(String name);
    List<WineTypeEntity> findAllByActiveTrue(); // Para getAll() por defecto
    List<WineTypeEntity> findAllByActiveFalse(); // Para un posible getInactive()
}