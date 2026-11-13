package com.utn.corralon.features.stockMovement.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.stockMovement.entity.StockMovementEntity;
import com.utn.corralon.features.stockMovement.enums.StockMovementType;
import com.utn.corralon.features.stockMovement.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockService stockService;

    private UUID variantId;

    @BeforeEach
    void setUp() {
        variantId = UUID.randomUUID();
    }

    @Test
    void registerEntry_shouldIncreaseStockAndRecordMovement() {
        ProductVariantEntity variant = variant(10, true);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        ProductVariantEntity result = stockService.registerEntry(
                variantId, 5, "Compra de mercaderia", 7L);

        assertThat(result).isSameAs(variant);
        assertThat(variant.getStock()).isEqualTo(15);

        StockMovementEntity movement = captureMovement();
        assertThat(movement.getType()).isEqualTo(StockMovementType.ENTRY);
        assertThat(movement.getQuantity()).isEqualTo(5);
        assertThat(movement.getResultingStock()).isEqualTo(15);
        assertThat(movement.getOrderId()).isNull();
        assertThat(movement.getOrderItemId()).isNull();
        assertThat(movement.getCreatedByUserId()).isEqualTo(7L);
        assertThat(movement.getVariant()).isSameAs(variant);
        assertThat(movement.getMovementDate()).isNotNull();

        verify(productVariantRepository).save(variant);
    }

    @Test
    void registerSale_shouldDecreaseStockAndTraceOrderItemAndUser() {
        ProductVariantEntity variant = variant(10, true);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        stockService.registerSale(variantId, 3, 100L, 200L, 5L,
                "Stock discounted by order creation");

        assertThat(variant.getStock()).isEqualTo(7);

        StockMovementEntity movement = captureMovement();
        assertThat(movement.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(movement.getQuantity()).isEqualTo(3);
        assertThat(movement.getResultingStock()).isEqualTo(7);
        assertThat(movement.getOrderId()).isEqualTo(100L);
        assertThat(movement.getOrderItemId()).isEqualTo(200L);
        assertThat(movement.getCreatedByUserId()).isEqualTo(5L);
    }

    @Test
    void registerSale_shouldRejectInsufficientStockWithoutSaving() {
        ProductVariantEntity variant = variant(2, true);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> stockService.registerSale(
                variantId, 3, 100L, 200L, 5L, "Stock discounted by order creation"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Insufficient stock");

        assertThat(variant.getStock()).isEqualTo(2);
        verify(productVariantRepository, never()).save(any(ProductVariantEntity.class));
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void registerSale_shouldRejectInactiveVariant() {
        ProductVariantEntity variant = variant(10, false);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> stockService.registerSale(
                variantId, 1, 100L, 200L, 5L, "Stock discounted by order creation"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Product variant is inactive");

        verify(productVariantRepository, never()).save(any(ProductVariantEntity.class));
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void registerAdjustment_shouldApplyPositiveDelta() {
        ProductVariantEntity variant = variant(10, true);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        stockService.registerAdjustment(variantId, 4, "Correccion de inventario", 9L);

        assertThat(variant.getStock()).isEqualTo(14);

        StockMovementEntity movement = captureMovement();
        assertThat(movement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(movement.getQuantity()).isEqualTo(4);
        assertThat(movement.getResultingStock()).isEqualTo(14);
        assertThat(movement.getCreatedByUserId()).isEqualTo(9L);
        assertThat(movement.getOrderId()).isNull();
    }

    @Test
    void registerAdjustment_shouldApplyNegativeDelta() {
        ProductVariantEntity variant = variant(10, true);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        stockService.registerAdjustment(variantId, -4, "Correccion de inventario", 9L);

        assertThat(variant.getStock()).isEqualTo(6);

        StockMovementEntity movement = captureMovement();
        assertThat(movement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(movement.getQuantity()).isEqualTo(-4);
        assertThat(movement.getResultingStock()).isEqualTo(6);
    }

    @Test
    void registerAdjustment_shouldRejectNegativeResultingStock() {
        ProductVariantEntity variant = variant(2, true);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> stockService.registerAdjustment(
                variantId, -3, "Correccion de inventario", 9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Stock cannot be negative");

        assertThat(variant.getStock()).isEqualTo(2);
        verify(productVariantRepository, never()).save(any(ProductVariantEntity.class));
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void registerAdjustment_shouldRejectZeroDelta() {
        assertThatThrownBy(() -> stockService.registerAdjustment(
                variantId, 0, "Correccion de inventario", 9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Adjustment quantity cannot be zero");

        verifyNoInteractions(productVariantRepository);
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void restoreStock_shouldIncreaseStockEvenIfVariantIsInactive() {
        ProductVariantEntity variant = variant(5, false);
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.of(variant));

        stockService.restoreStock(variantId, 2, 300L, 400L, 11L,
                "Stock restored by order cancellation");

        assertThat(variant.getStock()).isEqualTo(7);

        StockMovementEntity movement = captureMovement();
        assertThat(movement.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(movement.getQuantity()).isEqualTo(2);
        assertThat(movement.getResultingStock()).isEqualTo(7);
        assertThat(movement.getOrderId()).isEqualTo(300L);
        assertThat(movement.getOrderItemId()).isEqualTo(400L);
        assertThat(movement.getCreatedByUserId()).isEqualTo(11L);
    }

    @Test
    void registerEntry_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> stockService.registerEntry(
                variantId, 0, "Compra de mercaderia", 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Entry quantity must be greater than zero");

        verifyNoInteractions(productVariantRepository);
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void shouldRejectUnknownVariant() {
        when(productVariantRepository.findByExternalIdForUpdate(variantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.registerEntry(
                variantId, 1, "Compra de mercaderia", 7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(variantId.toString());

        verifyNoInteractions(stockMovementRepository);
    }

    private ProductVariantEntity variant(int stock, boolean active) {
        return ProductVariantEntity.builder()
                .externalId(variantId)
                .stock(stock)
                .active(active)
                .build();
    }

    private StockMovementEntity captureMovement() {
        ArgumentCaptor<StockMovementEntity> captor =
                ArgumentCaptor.forClass(StockMovementEntity.class);
        verify(stockMovementRepository).save(captor.capture());
        return captor.getValue();
    }
}
