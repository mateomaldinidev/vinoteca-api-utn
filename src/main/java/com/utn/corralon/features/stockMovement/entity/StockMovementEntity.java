package com.utn.corralon.features.stockMovement.entity;

import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.stockMovement.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "stockMovements")
public class StockMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "externalId", nullable = false, unique = true)
    private UUID externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stockMovementType", nullable = false)
    private StockMovementType type;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "movementDate", nullable = false)
    private LocalDateTime movementDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantId", nullable = false)
    private ProductVariantEntity variant;

    // Trazabilidad de pedido y autor (NULL en movimientos historicos o manuales).
    // Se mapean como IDs escalares: son columnas de auditoria opcionales, sin navegacion
    // de entidades en esta etapa. Las FKs y el CHECK compuesto los garantiza la base.
    @Column(name = "orderId")
    private Long orderId;

    @Column(name = "orderItemId")
    private Long orderItemId;

    @Column(name = "resultingStock")
    private Integer resultingStock;

    @Column(name = "createdByUserId")
    private Long createdByUserId;

}
