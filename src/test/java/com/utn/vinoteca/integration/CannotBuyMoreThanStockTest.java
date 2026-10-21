package com.utn.vinoteca.integration;

import com.utn.vinoteca.exception.StockInsufficientException;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CannotBuyMoreThanStockTest extends AbstractIntegrationTest {

    @Test
    void cartRejectsQuantityGreaterThanAvailableStock() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 5, null, null);

        assertThatThrownBy(() -> addToCart(user, variant, 6))
                .isInstanceOf(StockInsufficientException.class);

        assertThat(variant.getStock()).isEqualTo(5);
    }

    @Test
    void checkoutRejectsQuantityGreaterThanStockWithoutTouchingIt() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        addToCart(user, variant, 4);
        variant.setStock(3);
        wineVariantRepository.save(variant);

        assertThatThrownBy(() -> checkout(user))
                .isInstanceOf(com.utn.vinoteca.exception.StockInsufficientException.class);

        assertThat(variant.getStock()).isEqualTo(3);
    }
}
