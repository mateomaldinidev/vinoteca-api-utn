package com.utn.vinoteca.features.order.repository;

import com.utn.vinoteca.features.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByExternalId(UUID externalId);
    List<OrderEntity> findByUser_ExternalId(UUID externalId);

}
