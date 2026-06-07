package com.lorenzovendrame.orderservice.repository;

import com.lorenzovendrame.orderservice.domain.Order;
import com.lorenzovendrame.orderservice.domain.OrderItem;
import com.lorenzovendrame.orderservice.domain.enums.PaymentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface OrderMapper {

    void insertOrder(Order order);

    void insertOrderItem(OrderItem item);

    int updateOrderStatus(@Param("orderId") UUID orderId, @Param("status") PaymentStatus status);

    Optional<Order> findById(@Param("orderId") UUID orderId);

    List<OrderItem> findItemsByOrderId(@Param("orderId") UUID orderId);

}