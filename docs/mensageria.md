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

> Nesta etapa só a infraestrutura está pronta: nenhum caso de uso publica eventos e não há
> `ManipuladorEvento` registrado. Eventos recebidos são marcados como processados e logados
> com `[SAGA_EVENTO_SEM_MANIPULADOR]`.

## Peças

| Classe | Papel |
|---|---|
| `application/evento/gateway/PublicadorEventosGateway` | Porta de saída usada pelos casos de uso |
| `adapters/messaging/OutboxPublicadorEventos` | Grava o evento em `outbox_eventos` na transação do chamador |
| `adapters/messaging/OutboxRelay` | Publica os pendentes no SNS (atributo `eventType`) |
| `adapters/messaging/SqsConsumidorEventos` | Recebe da fila; só apaga depois de processar |
| `adapters/messaging/ProcessadorEventos` | Deduplica por `eventId` (`eventos_processados`) e chama o manipulador |
| `adapters/messaging/ManipuladorEvento` | Interface dos manipuladores, um por `eventType` |

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `MENSAGERIA_HABILITADA` | `false` | Liga relay e consumidor. Desligada, eventos ficam só na outbox |
| `MENSAGERIA_TOPICO_ARN` | — | `oficina-billing-events` |
| `MENSAGERIA_FILA_URL` | — | `oficina-billing-queue` |
| `MENSAGERIA_ENDPOINT` | — | Endpoint alternativo (LocalStack) |
| `AWS_REGION` | `us-east-1` | Região |

## Local e AWS

- `docker-compose up --build` sobe o LocalStack (`billing-localstack`, porta `LOCALSTACK_PORT`,
  padrão 4566) com a topologia completa criada por
  [`localstack/init-mensageria.sh`](../localstack/init-mensageria.sh).
- No deploy, o `deploy.yml` resolve ARN e URL pelo nome e injeta no `configmap-rds.yaml`.
  O `oficina-infra-cluster` (que cria tópicos e filas em `messaging.tf`) precisa ter sido
  aplicado antes.
