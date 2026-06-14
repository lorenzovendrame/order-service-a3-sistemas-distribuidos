package com.lorenzovendrame.orderservice.service;

import com.fasterxml.uuid.Generators;
import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.OrderItem;
import com.lorenzovendrame.orderservice.domain.enums.OrderStatus;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OrderTransactionService {

    private static final Logger log = LoggerFactory.getLogger(OrderTransactionService.class);

    private final OrderMapper orderMapper;

    public OrderTransactionService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Order saveOrderWithItems(Order order) {
        log.info("Iniciando transacao para salvar novo pedido e seus itens | userId: {} | eventId: {}", order.getUserId(), order.getEventId());

        if (order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("Transacao abortada: Tentativa de criar pedido sem itens | userId: {} | eventId: {}", order.getUserId(), order.getEventId());
            throw new BusinessException("Não é possível criar um pedido sem itens (assentos).");
        }

        UUID generatedOrderId = Generators.timeBasedEpochGenerator().generate();
        UUID generatedSagaId = Generators.timeBasedEpochGenerator().generate();

        order.setOrderId(generatedOrderId);
        order.setSagaId(generatedSagaId);
        order.setStatus(OrderStatus.PENDING);

        LocalDateTime timestamp = LocalDateTime.now();
        order.setCreatedAt(timestamp);
        order.setUpdatedAt(timestamp);

        orderMapper.insertOrder(order);
        log.info("Cabecalho do pedido inserido no banco | orderId: {} | sagaId: {}", generatedOrderId, generatedSagaId);

        for (OrderItem item : order.getItems()) {
            item.setOrderItemId(Generators.timeBasedEpochGenerator().generate());
            item.setOrderId(generatedOrderId);
            orderMapper.insertOrderItem(item);
            log.info("Item de pedido inserido no banco | orderId: {} | orderItemId: {} | seatIdentifier: {}",
                    generatedOrderId, item.getOrderItemId(), item.getSeatIdentifier());
        }

        log.info("Transacao concluida: Pedido e itens consolidados com sucesso | orderId: {}", generatedOrderId);
        return order;
    }
}