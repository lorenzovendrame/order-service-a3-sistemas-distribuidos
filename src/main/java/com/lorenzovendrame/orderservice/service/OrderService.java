package com.lorenzovendrame.orderservice.service;

import com.fasterxml.uuid.Generators;
import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.OrderItem;
import com.lorenzovendrame.orderservice.dto.OrderCreatedEvent;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.exception.OrderNotFoundException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.awspring.cloud.sqs.operations.SqsTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final SqsTemplate sqsTemplate;

    private static final String QUEUE_NAME = "fila-reserva-assentos.fifo";

    public OrderService(OrderMapper orderMapper, SqsTemplate sqsTemplate) {
        this.orderMapper = orderMapper;
        this.sqsTemplate = sqsTemplate;
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

        sendToSqs(generatedOrderId, generatedSagaId, order);

        return order;
    }

    public Order getOrderById(UUID orderId) {
        return orderMapper.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public void updateStatus(UUID orderId, String status) {
        int affectedRows = orderMapper.updateOrderStatus(orderId, status);
        if (affectedRows == 0) {
            throw new OrderNotFoundException(orderId);
        }
    }

    private void sendToSqs(UUID orderUuid, UUID sagaUuid, Order order) {
        List<OrderCreatedEvent.ItemEvent> itemEvents = order.getItems().stream()
                .map(item -> new OrderCreatedEvent.ItemEvent(
                        item.getTicketType(),
                        item.getQuantity(),
                        item.getSeatIdentifier()))
                .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
                sagaUuid.toString(),
                orderUuid.toString(),
                order.getUserId().toString(),
                order.getEventId().toString(),
                order.getTotalPrice(),
                itemEvents
        );

        sqsTemplate.send(to -> to
                .queue(QUEUE_NAME)
                .payload(event)
                .header("MessageGroupId", order.getEventId())
                .header("MessageDeduplicationId", sagaUuid.toString())
        );
    }
}