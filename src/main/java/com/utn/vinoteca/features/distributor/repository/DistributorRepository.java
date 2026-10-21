package com.utn.vinoteca.features.distributor.repository;

import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface DistributorRepository extends JpaRepository<DistributorEntity, Long> {

    Optional<DistributorEntity> findByExternalId(UUID externalId);
    Optional<DistributorEntity> findByName(String name);
    boolean existsByName(String name);

    Optional<DistributorEntity> findByExternalIdAndActiveTrue(UUID externalId);
    boolean existsByNameAndActiveTrue(String name);
    List<DistributorEntity> findAllByActiveTrue(); // Para getAll() por defecto
    List<DistributorEntity> findAllByActiveFalse(); // Para un posible getInactive()
}