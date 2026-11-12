# 📨 Mensageria (SNS + SQS)

Infraestrutura de eventos da Saga coreografada (ADR 005, em
[`oficina-app-api`](https://github.com/GrupoPosTech-FIAP/oficina-app-api/blob/main/docs/adr/005-saga-coreografada.md)).
É a mesma implementação do `oficina-app-api`; o guia completo (diagrama, como publicar e
consumir) está em
[`oficina-app-api/docs/mensageria.md`](https://github.com/GrupoPosTech-FIAP/oficina-app-api/blob/main/docs/mensageria.md).

## Neste serviço

| | |
|---|---|
| Publica em | `oficina-billing-events` |
| Consome | `oficina-billing-queue` (DLQ: `oficina-billing-dlq`) |
| Eventos que publica (ADR 005) | OrcamentoGerado, OrcamentoAprovado, OrcamentoNegado, OrcamentoExpirado, CobrancaCriada, PagamentoConfirmado, PagamentoRecusado, PagamentoExpirado, PagamentoEstornado |
| Eventos que recebe | DiagnosticoConcluido (os), EstoqueBaixado (inventory), ExecucaoFalhou (execution) |

### Passos da Saga

| Gatilho | Caso de uso | Resultado |
|---|---|---|
| `DiagnosticoConcluido` | `GerarOrcamentoDoDiagnosticoUseCase` | Gera e envia o orçamento → `OrcamentoGerado`. Se a OS já tem orçamento em andamento, ignora |
| `PATCH /orcamentos/{id}/aprovar` | `AprovarOrcamentoUseCase` | `OrcamentoAprovado` com os insumos a baixar |
| `PATCH /orcamentos/{id}/negar` | `NegarOrcamentoUseCase` | `OrcamentoNegado` (a OS é cancelada) |
| `EstoqueBaixado` | `CobrarOrdemServicoUseCase` | Cria a preferência no Checkout Pro → `CobrancaCriada` (com `checkoutUrl`) |
| Webhook `approved` | `ProcessarWebhookMercadoPagoUseCase` | Guarda o `paymentId` → `PagamentoConfirmado` |
| Webhook `rejected`/`cancelled` | idem | `PagamentoRecusado` (o inventory estorna os insumos) |
| Webhook `approved` com a cobrança já `CANCELADO` (expirada) ou `RECUSADO` | idem | A OS já foi cancelada: reembolsa no Mercado Pago, cobrança → `ESTORNADO` com o `paymentId` → `PagamentoEstornado` (`motivo` `PAGAMENTO_APOS_EXPIRACAO`/`PAGAMENTO_APOS_RECUSA`) |
| Webhook `approved` de um segundo pagamento da cobrança já paga | idem | Reembolsa só esse pagamento; a cobrança fica como está → `PagamentoEstornado` (`motivo` `PAGAMENTO_DUPLICADO`) |
| `ExecucaoFalhou` | `EstornarPagamentoOrdemServicoUseCase` | Reembolso no Mercado Pago (`X-Idempotency-Key` fixa por pagamento) → `PagamentoEstornado` |
| Agendador (1 min) | `ExpirarOrcamentosUseCase` | Orçamento sem resposta além de `ORCAMENTO_EXPIRACAO` → `EXPIRADO` + `OrcamentoExpirado` |
| Agendador (1 min) | `ExpirarCobrancasUseCase` | Cobrança `PENDENTE` além de `COBRANCA_EXPIRACAO` → `CANCELADO` + `PagamentoExpirado` |

A cobrança usa o id da OS como `pedidoId`, então há uma por OS. Falhas no Mercado Pago (sem
credencial, API fora) lançam exceção: a mensagem volta para a fila e, depois de 5 tentativas,
vai para a DLQ.

## Peças

| Classe | Papel |
|---|---|
| `application/evento/gateway/PublicadorEventosGateway` | Porta de saída usada pelos casos de uso |
| `adapters/messaging/OutboxPublicadorEventos` | Grava o evento em `outbox_eventos` na transação do chamador |
| `adapters/messaging/OutboxRelay` | Publica os pendentes no SNS (atributo `eventType`) |
| `adapters/messaging/SqsConsumidorEventos` | Recebe da fila; só apaga depois de processar |
| `adapters/messaging/ProcessadorEventos` | Deduplica por `eventId` (`eventos_processados`) e chama o manipulador |
| `adapters/messaging/ManipuladorEvento` | Interface dos manipuladores; cada um declara os `eventTypes` que trata |

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `MENSAGERIA_HABILITADA` | `false` | Liga relay e consumidor. Desligada, eventos ficam só na outbox |
| `MENSAGERIA_TOPICO_ARN` | — | `oficina-billing-events` |
| `MENSAGERIA_FILA_URL` | — | `oficina-billing-queue` |
| `MENSAGERIA_ENDPOINT` | — | Endpoint alternativo (LocalStack) |
| `AWS_REGION` | `us-east-1` | Região |
| `ORCAMENTO_EXPIRACAO` | `PT72H` | Prazo para o cliente responder o orçamento (ISO-8601; use `PT2M` na demo) |
| `COBRANCA_EXPIRACAO` | `PT24H` | Prazo para o pagamento da cobrança |

## Local e AWS

- `docker-compose up --build` sobe o LocalStack (`billing-localstack`, porta `LOCALSTACK_PORT`,
  padrão 4566) com a topologia completa criada por
  [`localstack/init-mensageria.sh`](../localstack/init-mensageria.sh).
- No deploy, o `deploy.yml` resolve ARN e URL pelo nome e injeta no `configmap-rds.yaml`.
  O `oficina-infra-cluster` (que cria tópicos e filas em `messaging.tf`) precisa ter sido
  aplicado antes.
