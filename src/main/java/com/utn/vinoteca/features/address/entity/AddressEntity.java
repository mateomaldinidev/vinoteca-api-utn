package com.utn.vinoteca.features.address.entity;

import com.utn.vinoteca.features.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "addresses")
@SQLRestriction("active = true")
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId", nullable = false, unique = true, updatable = false)
    @UuidGenerator
    private UUID externalId = UUID.randomUUID();

    @Column(name = "street", nullable = false)
    private String street;
    @Column(name = "streetNumber", nullable = false)
    private String streetNumber;
    @Column(name = "floor", nullable = true)
    private String floor;
    @Column(name = "apartmentNumber", nullable = true)
    private String apartmentNumber;
    @Column(name = "city", nullable = false)
    private String city;
    @Column(name = "zip_code", nullable = false)
    private String zipCode;
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}
