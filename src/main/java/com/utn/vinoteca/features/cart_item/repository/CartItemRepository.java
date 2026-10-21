package com.utn.vinoteca.features.cart_item.repository;

import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.cart_item.entity.CartItemEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity,Long> {
    // Metodo para encontrar un ítem del carrito por su externalId (UUID)
    Optional<CartItemEntity> findByExternalId(UUID externalId);

    // Metodo para encontrar un ítem del carrito por el carrito y el formato del vino
    Optional<CartItemEntity> findByCartAndWineVariant(CartEntity cart, WineVariantEntity wineVariant);

    // Metodo para encontrar un ítem del carrito por el carrito y el externalId del formato del vino
    Optional<CartItemEntity> findByCartAndWineVariantExternalId(CartEntity cart, UUID wineVariantExternalId);
}
