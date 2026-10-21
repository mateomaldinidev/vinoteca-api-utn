package com.utn.vinoteca.features.cartItem.entity;

import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
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
@Table(name="cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cart_id", "wine_variant_id"})
})
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="externalId",nullable = false,unique = true,updatable = false)
    private UUID externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="cart_id",nullable = false)
    private CartEntity cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="wine_variant_id",nullable = false)
    private WineVariantEntity wineVariant;

    @Column(name="quantity", nullable = false)
    private Integer quantity;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt; //me parece que es importante para auditar


    // Callbacks de JPA para gestionar campos automáticamente
    @PrePersist // Se ejecuta antes de que la entidad sea persistida por primera vez
    protected void onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID(); // Genera el UUID si no se ha establecido
        }
        createdAt = LocalDateTime.now(); // Establece la fecha de creación
    }

}
