package com.utn.corralon.features.stockMovement.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.stockMovement.entity.StockMovementEntity;
import com.utn.corralon.features.stockMovement.enums.StockMovementType;
import com.utn.corralon.features.stockMovement.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService implements IStockService {

    private final ProductVariantRepository productVariantRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public ProductVariantEntity registerEntry(UUID variantExternalId,
                                              Integer quantity,
                                              String reason,
                                              Long createdByUserId) {
        int entryQuantity = requirePositive(quantity, "Entry quantity");
        ProductVariantEntity variant = getActiveVariantForUpdate(variantExternalId);

        return applyMovement(variant, entryQuantity, entryQuantity, StockMovementType.ENTRY,
                reason, null, null, createdByUserId);
    }

    @Override
    @Transactional
    public ProductVariantEntity registerSale(UUID variantExternalId,
                                             Integer quantity,
                                             Long orderId,
                                             Long orderItemId,
                                             Long createdByUserId,
                                             String reason) {
        int saleQuantity = requirePositive(quantity, "Sale quantity");
        ProductVariantEntity variant = getVariantForUpdate(variantExternalId);

        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new BusinessRuleException("Product variant is inactive");
        }
        if (variant.getStock() < saleQuantity) {
            throw new BusinessRuleException("Insufficient stock");
        }

        return applyMovement(variant, -saleQuantity, saleQuantity, StockMovementType.SALE,
                reason, orderId, orderItemId, createdByUserId);
    }

    @Override
    @Transactional
    public ProductVariantEntity restoreStock(UUID variantExternalId,
                                             Integer quantity,
                                             Long orderId,
                                             Long orderItemId,
                                             Long createdByUserId,
                                             String reason) {
        int restoreQuantity = requirePositive(quantity, "Restore quantity");

        // Una devolucion debe poder reponer stock aunque la variante este inactiva.
        ProductVariantEntity variant = getVariantForUpdate(variantExternalId);

        return applyMovement(variant, restoreQuantity, restoreQuantity, StockMovementType.CANCELLATION,
                reason, orderId, orderItemId, createdByUserId);
    }

    @Override
    @Transactional
    public ProductVariantEntity registerAdjustment(UUID variantExternalId,
                                                   Integer quantityDelta,
                                                   String reason,
                                                   Long createdByUserId) {
        int delta = requireNonZero(quantityDelta, "Adjustment quantity");
        ProductVariantEntity variant = getActiveVariantForUpdate(variantExternalId);

        return applyMovement(variant, delta, delta, StockMovementType.ADJUSTMENT,
                reason, null, null, createdByUserId);
    }

    private ProductVariantEntity applyMovement(ProductVariantEntity variant,
                                               int stockDelta,
                                               int movementQuantity,
                                               StockMovementType type,
                                               String reason,
                                               Long orderId,
                                               Long orderItemId,
                                               Long createdByUserId) {
        int resultingStock = variant.getStock() + stockDelta;
        if (resultingStock < 0) {
            throw new BusinessRuleException("Stock cannot be negative");
        }

        variant.setStock(resultingStock);
        productVariantRepository.save(variant);

        StockMovementEntity movement = StockMovementEntity.builder()
                .movementDate(LocalDateTime.now())
                .quantity(movementQuantity)
                .type(type)
                .reason(reason)
                .variant(variant)
                .resultingStock(resultingStock)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .createdByUserId(createdByUserId)
                .build();

        stockMovementRepository.save(movement);
        return variant;
    }

    private ProductVariantEntity getActiveVariantForUpdate(UUID variantExternalId) {
        ProductVariantEntity variant = getVariantForUpdate(variantExternalId);
        if (!Boolean.TRUE.equals(variant.getActive())) {
            // Mantiene el comportamiento historico de buscar solo variantes activas.
            throw new ResourceNotFoundException("Product variant not found with ID:", variantExternalId);
        }
        return variant;
    }

    private ProductVariantEntity getVariantForUpdate(UUID variantExternalId) {
        return productVariantRepository.findByExternalIdForUpdate(variantExternalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product variant not found with ID:", variantExternalId));
    }

    private int requirePositive(Integer quantity, String label) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessRuleException(label + " must be greater than zero");
        }
        return quantity;
    }

    private int requireNonZero(Integer quantity, String label) {
        if (quantity == null || quantity == 0) {
            throw new BusinessRuleException(label + " cannot be zero");
        }
        return quantity;
    }
}
