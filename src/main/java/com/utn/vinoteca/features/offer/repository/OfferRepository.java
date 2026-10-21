package com.utn.vinoteca.features.offer.repository;

import com.utn.vinoteca.features.offer.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<OfferEntity, Long> {
    Optional<OfferEntity> findByExternalId(UUID externalId);
    Optional<OfferEntity> findByExternalIdAndActiveTrue(UUID externalId);
    List<OfferEntity> findAllByActiveTrue();
    List<OfferEntity> findAllByActiveFalse();
}
