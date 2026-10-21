package com.utn.vinoteca.features.wine.repository;

import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface WineRepository extends JpaRepository<WineEntity, Long>, JpaSpecificationExecutor<WineEntity> {

    Optional<WineEntity> findByExternalId(UUID externalId);
    Optional<WineEntity> findByExternalIdAndActiveTrue(UUID externalId);
    Optional<WineEntity> findByNameAndWinery(String name, WineryEntity winery);



    boolean existsByNameAndWinery(String name, WineryEntity winery);
}
