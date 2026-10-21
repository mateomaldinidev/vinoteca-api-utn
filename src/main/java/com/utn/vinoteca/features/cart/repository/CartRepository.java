package com.utn.vinoteca.features.cart.repository;

import com.utn.vinoteca.features.cart.entity.CartEntity;
import com.utn.vinoteca.features.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {

    // Metodo para encontrar un carrito por su externalId (UUID)
    Optional<CartEntity> findByExternalId(UUID externalId);

    // Metodo para encontrar un carrito por el externalId del usuario
    // Asume que UserEntity tiene un campo 'externalId'
    Optional<CartEntity> findByUserExternalId(UUID userExternalId);

    // Metodo para encontrar un carrito por la entidad User
    Optional<CartEntity> findByUser(UserEntity user);
}
