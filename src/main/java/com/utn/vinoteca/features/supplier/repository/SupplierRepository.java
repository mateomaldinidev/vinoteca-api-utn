package com.utn.vinoteca.features.supplier.repository;

import com.utn.vinoteca.features.supplier.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    Optional<SupplierEntity> findByExternalId(UUID externalId);
    boolean existsByName(String name);

    Optional<SupplierEntity> findByExternalIdAndActiveTrue(UUID externalId);
    boolean existsByNameAndActiveTrue(String name);
    List<SupplierEntity> findAllByActiveTrue(); // Para getAll() por defecto
    List<SupplierEntity> findAllByActiveFalse(); // Para un posible getInactive()
}