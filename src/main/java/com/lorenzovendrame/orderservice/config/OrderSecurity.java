package com.lorenzovendrame.orderservice.config;

import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.service.OrderService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component("orderSecurity")
public class OrderSecurity {

    private final OrderService orderService;

    public OrderSecurity(OrderService orderService) {
        this.orderService = orderService;
    }

    public boolean isOwner(String authenticatedUserId, String orderIdStr) {
        if (authenticatedUserId == null || orderIdStr == null) {
            return false;
        }

        try {
            UUID orderId = UUID.fromString(orderIdStr);

            Order order = orderService.getOrderById(orderId);

            return order.getUserId().toString().equals(authenticatedUserId);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}