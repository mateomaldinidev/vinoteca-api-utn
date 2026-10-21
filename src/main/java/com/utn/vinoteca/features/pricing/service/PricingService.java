package com.utn.vinoteca.features.pricing.service;

import com.utn.vinoteca.features.offerProduct.entity.OfferProductEntity;
import com.utn.vinoteca.features.offerProduct.repository.OfferProductRepository;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final OfferProductRepository offerProductRepository;

    public BigDecimal calculateUnitPrice(WineVariantEntity variant, int quantity) {
        return offerProductRepository.findActiveOfferForVariant(variant.getExternalId())
                .map(OfferProductEntity::getDiscountedPrice)
                .orElseGet(() -> calculateWithoutOffer(variant, quantity));
    }

    private BigDecimal calculateWithoutOffer(WineVariantEntity variant, int quantity) {
        boolean wholesaleApplies = variant.getWholesaleMinQty() != null
                && variant.getWholesalePrice() != null
                && quantity >= variant.getWholesaleMinQty();

        return wholesaleApplies
                ? variant.getWholesalePrice()
                : variant.getPrice();
    }
}
