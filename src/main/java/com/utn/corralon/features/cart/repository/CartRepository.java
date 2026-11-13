package com.utn.corralon.features.cart.repository;

import com.utn.corralon.features.cart.entity.CartEntity;
import com.utn.corralon.features.user.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Lock pesimista sobre el carrito para aislar concurrentemente el checkout
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartEntity c WHERE c.user = :user")
    Optional<CartEntity> findByUserForUpdate(@Param("user") UserEntity user);
}
