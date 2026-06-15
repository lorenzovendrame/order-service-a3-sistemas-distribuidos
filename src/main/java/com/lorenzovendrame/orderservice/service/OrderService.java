package com.lorenzovendrame.orderservice.service;

import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.enums.OrderStatus;
import com.lorenzovendrame.orderservice.dto.OrderCreatedEvent;
import com.lorenzovendrame.orderservice.exception.BusinessException;
import com.lorenzovendrame.orderservice.exception.OrderNotFoundException;
import com.lorenzovendrame.orderservice.repository.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.awspring.cloud.sqs.operations.SqsTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
        log.info("Iniciando validacao para criacao de novo pedido | userId: {} | eventId: {}", order.getUserId(), order.getEventId());

        if (!order.hasValidPaymentMethod()) {
            log.warn("Falha de validacao: Metodo de pagamento invalido | userId: {} | eventId: {}", order.getUserId(), order.getEventId());
            throw new BusinessException("Método de pagamento não encontrado.");
        }

        Order savedOrder = orderTransactionService.saveOrderWithItems(order);

        log.info("Pedido salvo no banco. Preparando para iniciar a Saga | orderId: {} | sagaId: {}", savedOrder.getOrderId(), savedOrder.getSagaId());
        sendToSqs(savedOrder.getOrderId(), savedOrder.getSagaId(), savedOrder);

        return savedOrder;
    }

    public Order getOrderById(UUID orderId) {
        log.info("Buscando pedido por ID no banco de dados | orderId: {}", orderId);
        return orderMapper.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Consulta falhou: Pedido nao encontrado | orderId: {}", orderId);
                    return new OrderNotFoundException(orderId);
                });
    }

    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status) {
        log.info("Iniciando atualizacao de status do pedido | orderId: {} | novoStatus: {}", orderId, status);
        int affectedRows = orderMapper.updateOrderStatus(orderId, status);

        if (affectedRows == 0) {
            log.warn("Falha ao atualizar status: Pedido nao encontrado no banco | orderId: {}", orderId);
            throw new OrderNotFoundException(orderId);
        }
        log.info("Status do pedido atualizado com sucesso no banco | orderId: {} | novoStatus: {}", orderId, status);
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
                order.getInstallments(),
                itemEvents
        );

        try {
            sqsTemplate.send(to -> to
                    .queue(QUEUE_NAME)
                    .payload(event)
                    .header("MessageGroupId", order.getEventId().toString())
                    .header("MessageDeduplicationId", sagaUuid.toString())
            );
            log.info("Primeiro evento da Saga publicado com sucesso (reserva de assentos) | queue: {} | orderId: {} | sagaId: {}",
                    QUEUE_NAME, orderUuid, sagaUuid);
        } catch (Exception e) {
            log.error("Falha critica ao publicar inicio da saga na fila SQS | queue: {} | orderId: {} | sagaId: {} | errorMessage: {}",
                    QUEUE_NAME, orderUuid, sagaUuid, e.getMessage(), e);
        }
    }
}