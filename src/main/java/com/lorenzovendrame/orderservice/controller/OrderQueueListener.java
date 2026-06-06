package com.lorenzovendrame.orderservice.controller;

import com.lorenzovendrame.orderservice.dto.OrderResponseEvent;
import com.lorenzovendrame.orderservice.service.OrderService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderQueueListener {
    // Implementar log e grafana posteriormente
    //private static final Logger log = LoggerFactory.getLogger(OrderQueueListener.class);
    private final OrderService orderService;

    public OrderQueueListener(OrderService orderService) {
        this.orderService = orderService;
    }
    // Fila disparada pelo Reserva de Assentos caso não houver estoque
    @SqsListener("fila-pedido-cancelado.fifo")
    public void handlePedidoSemEstoque(OrderResponseEvent event) {
        //log.warn("Saga {} - Pedido recusado por falta de assentos disponíveis. Motivo: {}", event.sagaId(), event.reason());

        UUID orderId = UUID.fromString(event.orderId());
        orderService.updateStatus(orderId, "CANCELED_NO_STOCK");
    }

    // Fila disparada pelo Pagamentos quando a cobrança falha
    @SqsListener("fila-pedido-compensado.fifo")
    public void handlePagamentoRecusado(OrderResponseEvent event) {
        //log.warn("Saga {} - Pedido cancelado devido a falha no pagamento. Motivo: {}", event.sagaId(), event.reason());

        UUID orderId = UUID.fromString(event.orderId());
        orderService.updateStatus(orderId, "CANCELED_PAYMENT_FAILED");
    }
    // Fila disparada pelo Pagamentos quando o pagamento for aprovado
    @SqsListener("fila-pedido-sucesso.fifo")
    public void handlePedidoSucesso(OrderResponseEvent event) {
        //log.info("Saga {} - 🎉 Pedido faturado e concluído com sucesso!", event.sagaId());

        UUID orderId = UUID.fromString(event.orderId());
        orderService.updateStatus(orderId, "COMPLETED");
    }
}
