package com.lorenzovendrame.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("Pedido com o ID " + id + " não foi encontrado.");
    }
}