package com.utn.corralon.features.pricing.service;

import com.utn.corralon.features.offer_product.repository.OfferProductRepository;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Calculo unico de precios de venta.
 *
 * Reglas comerciales (se mantienen sin cambios respecto de CartServiceImpl y
 * OrderService):
 * 1. Si la variante tiene una oferta activa, se usa el precio de la oferta.
 * 2. Si no hay oferta y la cantidad alcanza el minimo mayorista, se usa el
 *    precio mayorista.
 * 3. En cualquier otro caso, se usa el precio minorista.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final OfferProductRepository offerProductRepository;

    public BigDecimal calculateUnitPrice(ProductVariantEntity variant, Integer quantity) {

        BigDecimal offerPrice = offerProductRepository
                .findActiveOfferForVariant(variant.getExternalId())
                .map(offerProduct -> offerProduct.getDiscountedPrice())
                .orElse(null);

        if (offerPrice != null) {
            return offerPrice;
        }

        if (variant.getWholesaleMinQty() != null
                && variant.getWholesalePrice() != null
                && quantity != null
                && quantity >= variant.getWholesaleMinQty()) {
            return variant.getWholesalePrice();
        }

        return variant.getPrice();
    }
}
