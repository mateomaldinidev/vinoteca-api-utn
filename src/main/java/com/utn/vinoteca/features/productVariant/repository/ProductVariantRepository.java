package com.utn.vinoteca.features.productVariant.repository;

import com.utn.vinoteca.features.product.entity.ProductEntity;
import com.utn.vinoteca.features.productVariant.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long>, JpaSpecificationExecutor<ProductVariantEntity> {

    Optional<ProductVariantEntity> findByExternalId(UUID externalId);
    Optional<ProductVariantEntity> findByExternalIdAndActiveTrue(UUID externalId);

    boolean existsByProductAndAttribute(ProductEntity product, String attribute);

    Optional<ProductVariantEntity>findByProductAndAttribute(ProductEntity product, String attribute);
}
