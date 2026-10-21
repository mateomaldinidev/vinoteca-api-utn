package com.utn.vinoteca.features.stock.service;

import com.utn.vinoteca.features.stockMovement.entity.StockMovementEntity;
import com.utn.vinoteca.features.stockMovement.enums.StockMovementType;
import com.utn.vinoteca.features.stockMovement.repository.StockMovementRepository;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StockService {

    private final WineVariantRepository wineVariantRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public void discount(WineVariantEntity variant, int quantity, String reason) {
        variant.decrementStock(quantity);
        wineVariantRepository.save(variant);
        registerMovement(variant, quantity, StockMovementType.SALE, reason);
    }

    @Transactional
    public void restore(WineVariantEntity variant, int quantity, String reason) {
        variant.restoreStock(quantity);
        wineVariantRepository.save(variant);
        registerMovement(variant, quantity, StockMovementType.CANCELLATION, reason);
    }

    @Transactional
    public void registerEntry(WineVariantEntity variant, int quantity, String reason) {
        variant.restoreStock(quantity);
        wineVariantRepository.save(variant);
        registerMovement(variant, quantity, StockMovementType.ENTRY, reason);
    }

    @Transactional
    public void adjust(WineVariantEntity variant, int delta, String reason) {
        variant.adjustStock(delta);
        wineVariantRepository.save(variant);
        registerMovement(variant, delta, StockMovementType.ADJUSTMENT, reason);
    }

    private void registerMovement(WineVariantEntity variant,
                                  int quantity,
                                  StockMovementType type,
                                  String reason) {
        stockMovementRepository.save(StockMovementEntity.builder()
                .movementDate(LocalDateTime.now())
                .quantity(quantity)
                .type(type)
                .reason(reason)
                .variant(variant)
                .build());
    }
}
