package com.utn.corralon.features.stockMovement.service;

import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;

import java.util.UUID;

/**
 * Punto unico de modificacion de stock. Toda operacion actualiza la variante,
 * calcula el stock resultante y registra su movimiento en la misma transaccion.
 */
public interface IStockService {

    ProductVariantEntity registerEntry(UUID variantExternalId,
                                       Integer quantity,
                                       String reason,
                                       Long createdByUserId);

    ProductVariantEntity registerSale(UUID variantExternalId,
                                      Integer quantity,
                                      Long orderId,
                                      Long orderItemId,
                                      Long createdByUserId,
                                      String reason);

    ProductVariantEntity restoreStock(UUID variantExternalId,
                                      Integer quantity,
                                      Long orderId,
                                      Long orderItemId,
                                      Long createdByUserId,
                                      String reason);

    ProductVariantEntity registerAdjustment(UUID variantExternalId,
                                            Integer quantityDelta,
                                            String reason,
                                            Long createdByUserId);
}
