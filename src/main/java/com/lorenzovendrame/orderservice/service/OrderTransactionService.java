package com.lorenzovendrame.orderservice.service;

import com.fasterxml.uuid.Generators;
import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.OrderItem;
import com.lorenzovendrame.orderservice.domain.enums.PaymentStatus;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OrderTransactionService {

    private final OrderMapper orderMapper;

    public OrderTransactionService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Order saveOrderWithItems(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Não é possível criar um pedido sem itens (assentos).");
        }

        UUID generatedOrderId = Generators.timeBasedEpochGenerator().generate();
        UUID generatedSagaId = Generators.timeBasedEpochGenerator().generate();

        order.setOrderId(generatedOrderId);
        order.setSagaId(generatedSagaId);
        order.setStatus(PaymentStatus.PENDING);

        LocalDateTime timestamp = LocalDateTime.now();
        order.setCreatedAt(timestamp);
        order.setUpdatedAt(timestamp);

        orderMapper.insertOrder(order);

        for (OrderItem item : order.getItems()) {
            item.setOrderItemId(Generators.timeBasedEpochGenerator().generate());
            item.setOrderId(generatedOrderId);
            orderMapper.insertOrderItem(item);
        }

        return order;
    }
}