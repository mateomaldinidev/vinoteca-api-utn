package com.utn.vinoteca.features.offerProduct.repository;

import com.utn.vinoteca.features.offerProduct.entity.OfferProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferProductRepository extends JpaRepository<OfferProductEntity, Long> {
    Optional<OfferProductEntity> findByOfferExternalIdAndWineVariantExternalId(
            UUID offerExternalId, UUID wineVariantExternalId);
    boolean existsByOfferExternalIdAndWineVariantExternalId(
            UUID offerExternalId, UUID wineVariantExternalId);
    List<OfferProductEntity> findByOfferExternalId(UUID offerExternalId);

    @Query("SELECT op FROM OfferProductEntity op JOIN op.offer o " +
           "WHERE op.wineVariant.externalId = :variantId " +
           "AND o.active = true " +
           "AND o.startDate <= CURRENT_TIMESTAMP " +
           "AND o.endDate >= CURRENT_TIMESTAMP")
    Optional<OfferProductEntity> findActiveOfferForVariant(@Param("variantId") UUID variantId);
}
