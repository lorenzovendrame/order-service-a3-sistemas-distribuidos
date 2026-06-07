package com.lorenzovendrame.orderservice.controller;

import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Order> create(@RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(authentication.name, #id)")
    public ResponseEntity<Order> getById(@PathVariable String id) {
        UUID orderId = UUID.fromString(id);
        Order order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }
}