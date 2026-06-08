-- `order`.tb_orders definition

CREATE TABLE `tb_orders` (
                             `order_id` binary(16) NOT NULL,
                             `saga_id` binary(16) NOT NULL,
                             `user_id` binary(16) NOT NULL,
                             `event_id` binary(16) NOT NULL,
                             `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                             `payment_method` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                             `created_at` timestamp NULL DEFAULT NULL,
                             `updated_at` timestamp NULL DEFAULT NULL,
                             PRIMARY KEY (`order_id`),
                             UNIQUE KEY `uk_saga_id` (`saga_id`),
                             KEY `idx_orders_user_id` (`user_id`),
                             KEY `idx_orders_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- `order`.order_items definition

CREATE TABLE `order_items` (
                               `order_item_id` binary(16) NOT NULL,
                               `order_id` binary(16) NOT NULL,
                               `ticket_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                               `quantity` int NOT NULL,
                               `seat_identifier` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                               PRIMARY KEY (`order_item_id`),
                               KEY `idx_order_items_order_id` (`order_id`),
                               CONSTRAINT `fk_order_items_orders` FOREIGN KEY (`order_id`) REFERENCES `tb_orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;