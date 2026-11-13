package com.utn.corralon.features.pricing.service;

import com.utn.corralon.features.offer_product.entity.OfferProductEntity;
import com.utn.corralon.features.offer_product.repository.OfferProductRepository;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private OfferProductRepository offerProductRepository;

    @InjectMocks
    private PricingService pricingService;

    private ProductVariantEntity variant;

    @BeforeEach
    void setUp() {
        variant = ProductVariantEntity.builder()
                .externalId(UUID.randomUUID())
                .attribute("750ml")
                .price(new BigDecimal("100.00"))
                .wholesalePrice(new BigDecimal("80.00"))
                .wholesaleMinQty(10)
                .stock(100)
                .active(true)
                .build();
    }

    @Test
    void calculateUnitPrice_shouldUseActiveOfferEvenWhenQuantityReachesWholesaleMinimum() {
        OfferProductEntity activeOffer = OfferProductEntity.builder()
                .discountedPrice(new BigDecimal("70.00"))
                .build();

        when(offerProductRepository.findActiveOfferForVariant(variant.getExternalId()))
                .thenReturn(Optional.of(activeOffer));

        BigDecimal unitPrice = pricingService.calculateUnitPrice(variant, 20);

        assertThat(unitPrice).isEqualByComparingTo("70.00");
    }

    @Test
    void calculateUnitPrice_shouldUseRetailPriceWhenQuantityIsBelowWholesaleMinimum() {
        when(offerProductRepository.findActiveOfferForVariant(variant.getExternalId()))
                .thenReturn(Optional.empty());

        BigDecimal unitPrice = pricingService.calculateUnitPrice(variant, 9);

        assertThat(unitPrice).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateUnitPrice_shouldUseWholesalePriceWhenQuantityReachesWholesaleMinimum() {
        when(offerProductRepository.findActiveOfferForVariant(variant.getExternalId()))
                .thenReturn(Optional.empty());

        BigDecimal unitPrice = pricingService.calculateUnitPrice(variant, 10);

        assertThat(unitPrice).isEqualByComparingTo("80.00");
    }

    @Test
    void calculateUnitPrice_shouldUseRetailPriceWhenVariantHasNoWholesaleConfiguration() {
        ProductVariantEntity variantWithoutWholesale = ProductVariantEntity.builder()
                .externalId(UUID.randomUUID())
                .attribute("500ml")
                .price(new BigDecimal("50.00"))
                .stock(100)
                .active(true)
                .build();

        when(offerProductRepository.findActiveOfferForVariant(variantWithoutWholesale.getExternalId()))
                .thenReturn(Optional.empty());

        BigDecimal unitPrice = pricingService.calculateUnitPrice(variantWithoutWholesale, 100);

        assertThat(unitPrice).isEqualByComparingTo("50.00");
    }
}
