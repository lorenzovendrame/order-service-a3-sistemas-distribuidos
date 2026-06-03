package com.lorenzovendrame.orderservice.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.OrderItem;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.exception.OrderNotFoundException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    private final NoArgGenerator uuidV7Generator = Generators.timeBasedEpochGenerator();

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Não é possível criar um pedido sem itens (assentos).");
        }

        UUID generatedOrderId = uuidV7Generator.generate();
        UUID generatedSagaId = uuidV7Generator.generate();

        order.setOrderId(generatedOrderId);
        order.setSagaId(generatedSagaId);
        order.setStatus("PENDING"); // Inicia o estado da Saga

        orderMapper.insertOrder(order);

        for (OrderItem item : order.getItems()) {
            item.setOrderItemId(uuidV7Generator.generate());
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