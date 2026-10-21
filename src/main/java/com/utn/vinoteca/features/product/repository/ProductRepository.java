package com.utn.vinoteca.features.product.repository;

import com.utn.vinoteca.features.brand.entity.BrandEntity;
import com.utn.vinoteca.features.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByExternalId(UUID externalId);
    Optional<ProductEntity> findByExternalIdAndActiveTrue(UUID externalId);
    Optional<ProductEntity> findByNameAndBrand(String name, BrandEntity brand);



    boolean existsByNameAndBrand(String name, BrandEntity brand);
}
