package com.utn.vinoteca.features.address.repository;

import com.utn.vinoteca.features.address.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
    Optional<AddressEntity> findByExternalId(UUID externalId);
    //Accede al externalId de la entidad User relacionada
    List<AddressEntity> findAllByUser_ExternalId(UUID userExternalId);
}