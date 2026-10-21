package com.utn.vinoteca.features.offer_product.entity;

import com.utn.vinoteca.features.offer.entity.OfferEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "offer_products")
public class OfferProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private OfferEntity offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wine_variant_id", nullable = false)
    private WineVariantEntity wineVariant;

    @Column(name = "discounted_price",nullable = false, precision = 19, scale = 2)
    private BigDecimal discountedPrice;//agregamos esto porque el precio original esta en wineVatiant y el precio con descuento es distinto por oferta

}
