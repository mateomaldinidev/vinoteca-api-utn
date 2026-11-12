package com.utn.corralon.features.order.util;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.features.order.enums.DeliveryType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutCanonicalPayloadTest {

    private static final UUID ADDRESS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void buildCanonicalJson_forDelivery_shouldContainNormalizedDeliveryTypeAndAddress() {
        String canonical = CheckoutCanonicalPayload.buildCanonicalJson(
                DeliveryType.DELIVERY, ADDRESS_ID);

        assertThat(canonical).isEqualTo(
                "{\"deliveryType\":\"DELIVERY\",\"addressId\":\"00000000-0000-0000-0000-000000000001\"}");
    }

    @Test
    void buildCanonicalJson_forPickup_shouldContainNullAddress() {
        String canonical = CheckoutCanonicalPayload.buildCanonicalJson(
                DeliveryType.PICKUP, null);

        assertThat(canonical).isEqualTo("{\"deliveryType\":\"PICKUP\",\"addressId\":null}");
    }

    @Test
    void computeHash_shouldReturnKnownSha256HexVector() {
        assertThat(CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, ADDRESS_ID))
                .isEqualTo("dcdb2ef14c0f59e83a64ed8540fac0b16319e88de32cbda2f47f2647c3fc891c");

        assertThat(CheckoutCanonicalPayload.computeHash(DeliveryType.PICKUP, null))
                .isEqualTo("914b2d8e53b3422007d43cc1cd5002b78a7d84313ce8b632f49065d8a9417a64");
    }

    @Test
    void computeHash_shouldBeStableForSameParametersAndDifferForDifferentAddress() {
        String first = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, ADDRESS_ID);
        String second = CheckoutCanonicalPayload.computeHash(DeliveryType.DELIVERY, ADDRESS_ID);
        String differentAddress = CheckoutCanonicalPayload.computeHash(
                DeliveryType.DELIVERY,
                UUID.fromString("00000000-0000-0000-0000-000000000002"));

        assertThat(first).isEqualTo(second).hasSize(64);
        assertThat(first).isNotEqualTo(differentAddress);
    }

    @Test
    void buildCanonicalJson_whenDeliveryHasNoAddress_shouldThrowBusinessRuleException() {
        assertThatThrownBy(() -> CheckoutCanonicalPayload.buildCanonicalJson(DeliveryType.DELIVERY, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Address is required");
    }

    @Test
    void buildCanonicalJson_whenPickupHasAddress_shouldThrowBusinessRuleException() {
        assertThatThrownBy(() -> CheckoutCanonicalPayload.buildCanonicalJson(DeliveryType.PICKUP, ADDRESS_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Address must be null");
    }

    @Test
    void buildCanonicalJson_whenDeliveryTypeIsNull_shouldThrowBusinessRuleException() {
        assertThatThrownBy(() -> CheckoutCanonicalPayload.buildCanonicalJson(null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Delivery type is required");
    }
}
