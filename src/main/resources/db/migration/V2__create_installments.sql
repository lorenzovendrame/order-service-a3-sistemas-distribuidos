ALTER TABLE tb_orders
ADD installments INT NOT NULL DEFAULT 1
AFTER payment_method;