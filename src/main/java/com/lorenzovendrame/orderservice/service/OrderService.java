package com.lorenzovendrame.orderservice.service;

import com.fasterxml.uuid.Generators;
import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.OrderItem;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.exception.OrderNotFoundException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Não é possível criar um pedido sem itens (assentos).");
        }

        UUID generatedOrderId = Generators.timeBasedEpochGenerator().generate();
        UUID generatedSagaId = Generators.timeBasedEpochGenerator().generate();

        order.setOrderId(generatedOrderId);
        order.setSagaId(generatedSagaId);
        order.setStatus("PENDING"); // Inicia o estado da Saga
        LocalDateTime timestamp = LocalDateTime.now();
        order.setCreatedAt(timestamp);
        order.setUpdatedAt(timestamp);

        orderMapper.insertOrder(order);

        for (OrderItem item : order.getItems()) {
            item.setOrderItemId(Generators.timeBasedEpochGenerator().generate());
            item.setOrderId(generatedOrderId);
            orderMapper.insertOrderItem(item);
        }

        // 4. TODO: Postar o 'PedidoEvent' contendo o sagaId e orderId para a fila do Amazon SQS
        // queueMessagingTemplate.convertAndSend("fila-ingresso-reserva", event);

        return order;
    }

    public Order getOrderById(String orderId) {
        return orderMapper.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public void updateStatus(String orderId, String status) {
        int affectedRows = orderMapper.updateOrderStatus(orderId, status);
        if (affectedRows == 0) {
            throw new OrderNotFoundException(orderId);
        }
    }
}