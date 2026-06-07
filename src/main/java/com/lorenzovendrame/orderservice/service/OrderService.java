package com.lorenzovendrame.orderservice.service;

import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.enums.OrderStatus;
import com.lorenzovendrame.orderservice.dto.OrderCreatedEvent;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.exception.OrderNotFoundException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.awspring.cloud.sqs.operations.SqsTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final SqsTemplate sqsTemplate;
    private final OrderTransactionService orderTransactionService;

    public OrderService(OrderMapper orderMapper, SqsTemplate sqsTemplate, OrderTransactionService orderTransactionService) {
        this.orderMapper = orderMapper;
        this.sqsTemplate = sqsTemplate;
        this.orderTransactionService = orderTransactionService;
    }

    private static final String QUEUE_NAME = "fila-reserva-assentos.fifo";

    public Order createOrder(Order order) {

        if (!order.hasValidPaymentMethod()) {
            throw new BusinessException("Método de pagamento não encontrado.");
        }

        Order savedOrder = orderTransactionService.saveOrderWithItems(order);

        sendToSqs(savedOrder.getOrderId(), savedOrder.getSagaId(), savedOrder);

        return savedOrder;
    }

    public Order getOrderById(UUID orderId) {
        return orderMapper.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status) {
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
                order.getPaymentMethod().name(),
                itemEvents
        );

        sqsTemplate.send(to -> to
                .queue(QUEUE_NAME)
                .payload(event)
                .header("MessageGroupId", order.getEventId().toString())
                .header("MessageDeduplicationId", sagaUuid.toString())
        );
    }
}