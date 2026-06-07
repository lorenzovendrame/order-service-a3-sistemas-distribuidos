package com.lorenzovendrame.orderservice.dto;

import java.util.List;

public record OrderCreatedEvent(
        String sagaId,
        String orderId,
        String userId,
        String eventId,
        String paymentMethod,
        List<ItemEvent> items
) {
    public record ItemEvent(
            String ticketType,
            Integer quantity,
            String seatIdentifier
    ) {}
}
