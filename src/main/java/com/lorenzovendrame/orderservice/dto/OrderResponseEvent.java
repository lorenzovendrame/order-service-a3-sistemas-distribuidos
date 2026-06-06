package com.lorenzovendrame.orderservice.dto;

public record OrderResponseEvent(
        String sagaId,
        String orderId,
        String reason // Opcional: para logs de auditoria (ex: "Cartão Recusado", "Estoque Esgotado")
) {}