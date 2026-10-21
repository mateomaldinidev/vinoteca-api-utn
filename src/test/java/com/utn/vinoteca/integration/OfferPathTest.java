package com.utn.vinoteca.integration;

import com.utn.vinoteca.exception.BadRequestException;
import com.utn.vinoteca.features.offer.dto.OfferRequestDTO;
import com.utn.vinoteca.features.offer.dto.OfferResponseDTO;
import com.utn.vinoteca.features.offer.service.IOfferService;
import com.utn.vinoteca.features.offer_product.dto.OfferProductRequestDTO;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfferPathTest extends AbstractIntegrationTest {

    @Autowired
    private IOfferService offerService;

    @Test
    void pathOfferIdIsAuthoritativeAndMismatchesAreRejected() {
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        OfferResponseDTO offerA = offerService.create(offerRequest("Offer path A"));
        OfferResponseDTO offerB = offerService.create(offerRequest("Offer path B"));

        OfferProductRequestDTO mismatched = OfferProductRequestDTO.builder()
                .offerId(offerB.externalId())
                .wineVariantId(variant.getExternalId())
                .discountedPrice(new BigDecimal("90.00"))
                .build();

        assertThatThrownBy(() -> offerService.addProductToOffer(offerA.externalId(), mismatched))
                .isInstanceOf(BadRequestException.class);

        OfferProductRequestDTO valid = OfferProductRequestDTO.builder()
                .offerId(offerA.externalId())
                .wineVariantId(variant.getExternalId())
                .discountedPrice(new BigDecimal("90.00"))
                .build();

        OfferResponseDTO updated = offerService.addProductToOffer(offerA.externalId(), valid);

        assertThat(updated.offerProducts())
                .anyMatch(product -> product.wineVariantExternalId().equals(variant.getExternalId()));

        assertThat(offerService.getByExternalId(offerB.externalId()).offerProducts()).isEmpty();
    }

    private OfferRequestDTO offerRequest(String name) {
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        return OfferRequestDTO.builder()
                .name(name)
                .discountPercentage(new BigDecimal("10.00"))
                .startDate(start)
                .endDate(start.plusDays(30))
                .build();
    }
}
