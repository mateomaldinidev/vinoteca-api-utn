package com.utn.corralon.features.order.util;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.features.order.enums.DeliveryType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class CheckoutCanonicalPayload {

    private CheckoutCanonicalPayload() {}

    public static String buildCanonicalJson(DeliveryType deliveryType, UUID addressId) {
        if (deliveryType == null) {
            throw new BusinessRuleException("Delivery type is required");
        }
        String normalizedDeliveryType = deliveryType.name().trim().toUpperCase();

        if (deliveryType == DeliveryType.DELIVERY) {
            if (addressId == null) {
                throw new BusinessRuleException("Address is required for DELIVERY orders");
            }
            return String.format("{\"deliveryType\":\"%s\",\"addressId\":\"%s\"}",
                    normalizedDeliveryType, addressId.toString().toLowerCase());
        } else if (deliveryType == DeliveryType.PICKUP) {
            if (addressId != null) {
                throw new BusinessRuleException("Address must be null for PICKUP orders");
            }
            return String.format("{\"deliveryType\":\"%s\",\"addressId\":null}",
                    normalizedDeliveryType);
        } else {
            throw new BusinessRuleException("Unsupported delivery type: " + deliveryType);
        }
    }

    public static String computeHash(DeliveryType deliveryType, UUID addressId) {
        String canonicalJson = buildCanonicalJson(deliveryType, addressId);
        return sha256Hex(canonicalJson);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
