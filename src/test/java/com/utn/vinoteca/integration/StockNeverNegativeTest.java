package com.utn.vinoteca.integration;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockNeverNegativeTest extends AbstractIntegrationTest {

    @Test
    void sellingReducesStockButNeverBelowZero() {
        UserEntity user = createUser();
        WineVariantEntity variant = createVariant(money("100.00"), 10, null, null);

        addToCart(user, variant, 4);
        checkout(user);

        assertThat(variant.getStock()).isEqualTo(6).isGreaterThanOrEqualTo(0);
    }

    @Test
    void oversellingIsRejectedAndLeavesStockUntouched() {
        UserEntity seller = createUser();
        UserEntity lateBuyer = createUser();

        WineVariantEntity soldVariant = createVariant(money("100.00"), 10, null, null);
        WineVariantEntity contestedVariant = createVariant(money("100.00"), 10, null, null);

        addToCart(seller, soldVariant, 4);
        checkout(seller);
        assertThat(soldVariant.getStock()).isEqualTo(6);

        addToCart(lateBuyer, contestedVariant, 4);
        contestedVariant.setStock(3);
        wineVariantRepository.save(contestedVariant);

        assertThatThrownBy(() -> checkout(lateBuyer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(contestedVariant.getStock()).isEqualTo(3).isGreaterThanOrEqualTo(0);
    }
}
