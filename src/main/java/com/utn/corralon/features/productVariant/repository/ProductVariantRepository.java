package com.utn.corralon.features.productVariant.repository;

import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long>, JpaSpecificationExecutor<ProductVariantEntity> {

    Optional<ProductVariantEntity> findByExternalId(UUID externalId);
    Optional<ProductVariantEntity> findByExternalIdAndActiveTrue(UUID externalId);

    // Bloqueo pesimista para que el calculo de stock y resultingStock sea atomico.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariantEntity v WHERE v.externalId = :externalId")
    Optional<ProductVariantEntity> findByExternalIdForUpdate(@Param("externalId") UUID externalId);

    boolean existsByProductAndAttribute(ProductEntity product, String attribute);

    Optional<ProductVariantEntity>findByProductAndAttribute(ProductEntity product, String attribute);
}
