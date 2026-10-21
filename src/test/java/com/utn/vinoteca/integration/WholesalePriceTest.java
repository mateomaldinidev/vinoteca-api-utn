package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WholesalePriceTest extends AbstractIntegrationTest {

    @Test
    void appliesWholesalePriceFromMinimumQuantity() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 100, money("80.00"), 6);

        addToCart(user, variant, 6);
        OrderResponseDTO order = checkout(user);

        assertThat(order.getTotal()).isEqualByComparingTo("480.00");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("80.00");
        assertThat(order.getItems().get(0).getSubtotal()).isEqualByComparingTo("480.00");
    }

    @Test
    void usesRetailPriceBelowMinimumQuantity() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 100, money("80.00"), 6);

        addToCart(user, variant, 5);
        OrderResponseDTO order = checkout(user);

        assertThat(order.getTotal()).isEqualByComparingTo("500.00");
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("100.00");
    }
}
