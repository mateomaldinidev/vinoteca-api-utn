package com.utn.corralon.features.order.repository;

import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.user.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByExternalId(UUID externalId);
    List<OrderEntity> findByUser_ExternalId(UUID externalId);
    // Carga anticipada de las relaciones necesarias para mapear la respuesta
    // del replay idempotente sin depender del contexto web (OSIV).
    @EntityGraph(attributePaths = {"items", "items.order", "items.productVariant", "user", "address"})
    Optional<OrderEntity> findByUserAndIdempotencyKey(UserEntity user, String idempotencyKey);
}
