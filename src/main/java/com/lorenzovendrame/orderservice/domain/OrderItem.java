package com.lorenzovendrame.orderservice.domain;

import java.util.UUID;

public class OrderItem {
    private UUID orderItemId;
    private UUID orderId;
    private String ticketType;  // "MEIA", "INTEIRA"
    private Integer quantity;
    private String seatIdentifier;

    public OrderItem() {}

    public UUID getOrderItemId() { return orderItemId; }
    public void setOrderItemId(UUID orderItemId) { this.orderItemId = orderItemId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSeatIdentifier() { return seatIdentifier; }
    public void setSeatIdentifier(String seatIdentifier) { this.seatIdentifier = seatIdentifier; }
}
