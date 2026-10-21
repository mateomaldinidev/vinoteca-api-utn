package com.utn.vinoteca.features.stockMovement.repository;

import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.stockMovement.entity.StockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long>, JpaSpecificationExecutor<StockMovementEntity> {
    List<StockMovementEntity> findByVariantExternalId(UUID variantId);
    StockMovementEntity findByExternalId(UUID externalId);
}
