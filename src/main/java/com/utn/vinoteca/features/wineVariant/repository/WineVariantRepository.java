package com.utn.vinoteca.features.wineVariant.repository;

import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WineVariantRepository extends JpaRepository<WineVariantEntity, Long>, JpaSpecificationExecutor<WineVariantEntity> {

    Optional<WineVariantEntity> findByExternalId(UUID externalId);
    Optional<WineVariantEntity> findByExternalIdAndActiveTrue(UUID externalId);

    boolean existsByWineAndAttribute(WineEntity wine, String attribute);

    Optional<WineVariantEntity>findByWineAndAttribute(WineEntity wine, String attribute);
}
