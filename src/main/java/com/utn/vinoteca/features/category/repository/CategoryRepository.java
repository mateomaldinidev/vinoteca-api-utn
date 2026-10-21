package com.utn.vinoteca.features.category.repository;

import com.utn.vinoteca.features.category.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByExternalId(UUID externalId);
    boolean existsByName(String name); // Añadir este metodo para validacion de nombre

    // Nuevos metodos para consistencia con el campo 'active'
    Optional<CategoryEntity> findByExternalIdAndActiveTrue(UUID externalId);
    boolean existsByNameAndActiveTrue(String name);
    List<CategoryEntity> findAllByActiveTrue(); // Para getAll() por defecto
    List<CategoryEntity> findAllByActiveFalse(); // Para un posible getInactive()
}