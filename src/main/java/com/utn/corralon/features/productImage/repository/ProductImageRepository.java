package com.utn.corralon.features.productImage.repository;

import com.utn.corralon.features.productImage.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductExternalIdOrderByPrincipalDescIdAsc(UUID productExternalId);

    long countByProductExternalId(UUID productExternalId);

    Optional<ProductImageEntity> findByExternalIdAndProductExternalId(UUID externalId, UUID productExternalId);

    Optional<ProductImageEntity> findFirstByProductExternalIdAndPrincipalTrue(UUID productExternalId);

    Optional<ProductImageEntity> findFirstByProductExternalIdOrderByIdAsc(UUID productExternalId);
}
