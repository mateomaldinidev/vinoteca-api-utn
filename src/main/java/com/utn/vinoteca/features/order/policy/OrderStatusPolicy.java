package com.utn.vinoteca.features.order.policy;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.features.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusPolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public void assertTransition(OrderStatus from, OrderStatus to) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessRuleException(
                    "Invalid order status transition: " + from + " -> " + to);
        }
    }

    public void assertCanPay(OrderStatus current) {
        assertTransition(current, OrderStatus.PAID);
    }

    public void assertCanCancel(OrderStatus current) {
        assertTransition(current, OrderStatus.CANCELLED);
    }
}
