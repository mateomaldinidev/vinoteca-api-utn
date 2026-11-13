package com.utn.corralon.features.order.repository;

import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.user.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // Bloqueo pesimista: pago, cancelacion manual y vencimiento compiten por
    // la misma fila y revalidan el estado despues de obtener el lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.externalId = :externalId")
    Optional<OrderEntity> findByExternalIdForUpdate(@Param("externalId") UUID externalId);

    // Proyeccion de solo lectura para autorizar la cancelacion manual sin
    // cargar el pedido en el contexto de persistencia antes del lock.
    @Query("SELECT o.user.email FROM OrderEntity o WHERE o.externalId = :externalId")
    Optional<String> findOwnerEmailByExternalId(@Param("externalId") UUID externalId);

    @Query("SELECT o.externalId FROM OrderEntity o "
            + "WHERE o.status = :status "
            + "AND o.expiresAt IS NOT NULL "
            + "AND o.expiresAt <= :referenceTime")
    List<UUID> findExpiredOrderExternalIds(@Param("status") OrderStatus status,
                                           @Param("referenceTime") LocalDateTime referenceTime);
}
