# Order Service

Serviço de pedidos do sistema de reservas de eventos. Recebe pedidos de compra de tickets, persiste o pedido e dispara a saga de processamento (reserva de assentos → pagamento), atualizando o status do pedido conforme o resultado da saga.

## Stack

- Java 21 + Spring Boot
- Spring Security (JWT + controle de propriedade do recurso via `OrderSecurity`)
- MyBatis + MySQL + Flyway
- Spring Cloud AWS SQS (publica e consome filas FIFO da saga)
- Spring Boot Actuator + Micrometer/Prometheus

## Responsabilidades

- Criação de pedidos (`status = PENDING`) com itens (tipo de ticket, quantidade, assento)
- Validação de método de pagamento (`BOLETO`, `PIX`, `CREDIT_CARD`)
- Disparo do primeiro passo da saga: publica `OrderCreatedEvent` para reserva de assentos
- Consulta de pedido por ID
- Atualização de status do pedido conforme retorno da saga (`APPROVED`, `REJECTED`, `CANCELLED`)

## Endpoints

| Método | Rota | Autorização | Descrição |
|---|---|---|---|
| POST | `/v1/orders` | `USER` ou `ADMIN` | Cria pedido e inicia a saga |
| GET | `/v1/orders/{id}` | `ADMIN` ou dono do pedido (`OrderSecurity.isOwner`) | Retorna pedido por ID |

## Modelo de domínio

`Order`: `orderId`, `sagaId`, `userId`, `eventId`, `paymentMethod`, `installments`, `status`, `items` (lista de `OrderItem`: tipo de ticket, quantidade, assento).

`OrderStatus`: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.

## Fluxo da saga (pedido)

1. **Criação**: pedido salvo como `PENDING`. Order Service publica `OrderCreatedEvent` na fila `fila-reserva-assentos.fifo` (`MessageGroupId = eventId`, `MessageDeduplicationId = sagaId`).
2. **Reserva de assentos** (ticket-reservation-service) processa o pedido:
    - sucesso → segue para pagamento;
    - sem estoque → publica em `fila-pedido-cancelado.fifo`.
3. **Pagamento** (payment-service) processa:
    - aprovado → publica em `fila-pedido-sucesso.fifo` (e confirma reserva);
    - recusado → publica em `fila-pedido-compensado.fifo` (e compensa reserva).
4. Order Service escuta as três filas de retorno e atualiza o status final do pedido:

| Fila consumida | Novo status |
|---|---|
| `fila-pedido-cancelado.fifo` | `REJECTED` (sem estoque/já processado) |
| `fila-pedido-compensado.fifo` | `CANCELLED` (pagamento recusado) |
| `fila-pedido-sucesso.fifo` | `APPROVED` (pagamento aprovado) |

## Configuração (variáveis de ambiente)

| Variável | Descrição |
|---|---|
| `SERVER_PORT` | Porta do serviço (default `8080`) |
| `PROFILE` | Perfil ativo do Spring |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Conexão MySQL |
| `JWT_SECRET` | Segredo JWT compartilhado |
| `AWS_REGION` | Região AWS (default `us-east-1`) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | Credenciais AWS para SQS |

## Banco de dados

- Flyway (`db/migration`, `baseline-on-migrate=true`).
- Mappers MyBatis em `classpath:repository/**/*.xml`.
- `UuidBinaryTypeHandler` mapeia `UUID` ↔ `BINARY(16)`.

## Observabilidade

Actuator: `health`, `info`, `prometheus`, com tag `application=order-service`.

## Execução local

```bash
docker build -t order-service .
docker run -p 8080:8080 \
  -e PROFILE=dev \
  -e DATABASE_URL=jdbc:mysql://localhost:3306/orders \
  -e DATABASE_USERNAME=root \
  -e DATABASE_PASSWORD=secret \
  -e JWT_SECRET=<segredo-compartilhado> \
  -e AWS_ACCESS_KEY_ID=... \
  -e AWS_SECRET_ACCESS_KEY=... \
  order-service
```

## Papel na arquitetura

Orquestrador inicial e "fonte da verdade" do status do pedido na saga coreografada (event-driven via SQS). Não chama outros serviços diretamente — toda comunicação acontece por filas FIFO.
