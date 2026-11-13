package com.utn.corralon.features.order.entity;

import com.utn.corralon.features.address.entity.AddressEntity;
import com.utn.corralon.features.order.enums.DeliveryType;
import com.utn.corralon.features.order.enums.OrderStatus;
import com.utn.corralon.features.orderItem.entity.OrderItemEntity;
import com.utn.corralon.features.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="externalId",nullable = false,unique = true,updatable = false)
    @UuidGenerator
    private UUID externalId= UUID.randomUUID();

    @Column(name="total",nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name="createdAt",nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "cancellationReason", length = 50)
    private String cancellationReason;

    @Column(name = "cancelledAt")
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType;

    // Snapshot de la direccion al momento del checkout (NULL en PICKUP e historicos).
    @Column(name = "deliveryStreet")
    private String deliveryStreet;

    @Column(name = "deliveryStreetNumber")
    private String deliveryStreetNumber;

    @Column(name = "deliveryFloor")
    private String deliveryFloor;

    @Column(name = "deliveryApartmentNumber")
    private String deliveryApartmentNumber;

    @Column(name = "deliveryCity")
    private String deliveryCity;

    @Column(name = "deliveryZipCode")
    private String deliveryZipCode;

    // Idempotencia del checkout (NULL en pedidos historicos).
    @Column(name = "idempotencyKey", length = 255)
    private String idempotencyKey;

    @Column(name = "checkoutRequestHash", length = 128)
    private String checkoutRequestHash;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="userId",nullable = false)
    private UserEntity user;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name="addressId", nullable = true)
    private AddressEntity address;

}
