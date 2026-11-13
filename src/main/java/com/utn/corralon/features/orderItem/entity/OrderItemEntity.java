package com.utn.corralon.features.orderItem.entity;

import com.utn.corralon.features.order.entity.OrderEntity;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="orderItems")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="externalId",nullable = false,unique = true,updatable = false)
    @UuidGenerator
    private UUID externalId= UUID.randomUUID();

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="order_id",nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_variant_id",nullable = false)
    private ProductVariantEntity productVariant;

    // Snapshots al momento de crear el pedido (NULL en pedidos historicos).
    @Column(name = "productNameSnapshot", length = 255)
    private String productNameSnapshot;

    @Column(name = "variantAttributeSnapshot", length = 255)
    private String variantAttributeSnapshot;

    @Column(name="quantity",nullable = false)
    private Integer quantity;

    @Column(name="unit_price",nullable = false,precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name="subtotal",nullable = false,precision = 19, scale = 2)
    private BigDecimal subtotal; // SE LO SUMAMOS AUNQUE NO ESTE EN EL DER PORQUE ES IMPORTANTE YA QUE HAY QUE TENER UN HISTORIAL DE PRECIOS
}
