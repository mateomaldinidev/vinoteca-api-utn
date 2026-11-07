package com.utn.corralon.features.product.repository;

import com.utn.corralon.features.brand.entity.BrandEntity;
import com.utn.corralon.features.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByExternalId(UUID externalId);
    Optional<ProductEntity> findByExternalIdAndActiveTrue(UUID externalId);
    Optional<ProductEntity> findByNameAndBrand(String name, BrandEntity brand);



    boolean existsByNameAndBrand(String name, BrandEntity brand);

    boolean existsByExternalId(UUID externalId);

    // Productos relacionados a una categoria, marca o proveedor.
    List<ProductEntity> findByCategory_ExternalId(UUID categoryExternalId);

    List<ProductEntity> findByBrand_ExternalId(UUID brandExternalId);

    List<ProductEntity> findBySupplier_ExternalId(UUID supplierExternalId);

    long countByCategory_ExternalId(UUID categoryExternalId);

    long countByBrand_ExternalId(UUID brandExternalId);

    long countBySupplier_ExternalId(UUID supplierExternalId);
}
