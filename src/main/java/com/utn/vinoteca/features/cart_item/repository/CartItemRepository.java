package com.utn.vinoteca.features.cart_item.repository;

import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.cart_item.entity.CartItemEntity;
import com.utn.vinoteca.features.productVariant.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity,Long> {
    // Metodo para encontrar un ítem del carrito por su externalId (UUID)
    Optional<CartItemEntity> findByExternalId(UUID externalId);

    // Metodo para encontrar un ítem del carrito por el carrito y la variante del producto
    Optional<CartItemEntity> findByCartAndProductVariant(CartEntity cart, ProductVariantEntity productVariant);

    // Metodo para encontrar un ítem del carrito por el carrito y el externalId de la variante del producto
    Optional<CartItemEntity> findByCartAndProductVariantExternalId(CartEntity cart, UUID productVariantExternalId);
}
