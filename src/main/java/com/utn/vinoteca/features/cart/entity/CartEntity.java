package com.utn.vinoteca.features.cart.entity;


import com.utn.vinoteca.features.cart_item.entity.CartItemEntity;
import com.utn.vinoteca.features.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name="carts")
public class CartEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="externalId",nullable = false,unique = true,updatable = false)
    private UUID externalId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false, unique = true)
    private UserEntity user;

    @Column(name="last_updated",nullable = false)
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "cart",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<CartItemEntity> cartItems;


    public void addCartItem(CartItemEntity cartItem) {
        cartItems.add(cartItem);
        cartItem.setCart(this); // Sincroniza el lado ManyToOne
    }

    public void removeCartItem(CartItemEntity cartItem) {
        cartItems.remove(cartItem);
        cartItem.setCart(null); // Desvincula el lado ManyToOne
    }

    // Callbacks de JPA para gestionar campos automáticamente
    @PrePersist // Se ejecuta antes de que la entidad sea persistida por primera vez
    protected void onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID(); // Genera el UUID si no se ha establecido
        }
        lastUpdated = LocalDateTime.now(); // Establece la fecha de creación
    }


    @PreUpdate // Se ejecuta antes de que la entidad sea actualizada
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now(); // Actualiza la fecha de última modificación
    }
}
