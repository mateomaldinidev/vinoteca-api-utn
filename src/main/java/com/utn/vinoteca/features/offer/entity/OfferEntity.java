package com.utn.vinoteca.features.offer.entity;

import com.utn.vinoteca.features.offerProduct.entity.OfferProductEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="offers")
public class OfferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="externalId",nullable = false,unique = true,updatable = false)
    @UuidGenerator
    private UUID externalId = UUID.randomUUID();

    @Column(name="name",nullable = false)
    private String name;

    @Column(name = "discount_percentage",nullable = false,precision = 19, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name="start_date",nullable = false)
    private LocalDateTime startDate;

    @Column(name="end_date",nullable = false)
    private LocalDateTime endDate;

    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;

    @Column(name="active",nullable = false)
    private Boolean active;

    @Builder.Default
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferProductEntity> offerProducts = new ArrayList<>();


}
