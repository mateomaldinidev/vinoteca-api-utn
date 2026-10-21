package com.utn.vinoteca.features.payment.repository;

import com.utn.vinoteca.features.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    @Query("SELECT p FROM PaymentEntity p JOIN FETCH p.order WHERE p.externalId = :id")
    Optional<PaymentEntity> findByExternalIdWithOrder(UUID id);

    Optional<PaymentEntity> findByOrder_ExternalId(UUID orderExternalId);
}
