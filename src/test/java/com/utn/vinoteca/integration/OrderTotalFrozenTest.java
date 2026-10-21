package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.entity.OrderEntity;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTotalFrozenTest extends AbstractIntegrationTest {

    @Test
    void orderKeepsItsTotalAndUnitPriceAfterPriceChanges() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        addToCart(user, variant, 2);
        OrderResponseDTO order = checkout(user);

        variant.setPrice(money("150.00"));
        wineVariantRepository.save(variant);

        OrderEntity stored = orderRepository.findByExternalId(order.getExternalId()).orElseThrow();

        assertThat(stored.getTotal()).isEqualByComparingTo("200.00");
        assertThat(stored.getItems()).hasSize(1);
        assertThat(stored.getItems().get(0).getUnitPrice()).isEqualByComparingTo("100.00");
        assertThat(stored.getItems().get(0).getSubtotal()).isEqualByComparingTo("200.00");
    }
}
