package com.lorenzovendrame.orderservice.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String id) {
        super("Pedido com o ID " + id + " não foi encontrado.");
    }
}