# Oficina Billing Service

Microsserviço de **Faturamento e Cobranças** da oficina mecânica, responsável pela gestão do ciclo de vida de **Orçamentos**, geração de links de pagamento via **Mercado Pago Checkout Pro (API Headless)** e conciliação financeira assíncrona por meio de **Webhooks com validação criptográfica HMAC-SHA256**.

Desenvolvido como parte do ecossistema de microsserviços da pós-graduação **SOAT (Software Architecture) da FIAP**.

---

## 🛠️ Stack Tecnológica

| Componente | Tecnologia | Versão / Detalhes |
| :--- | :--- | :--- |
| **Linguagem** | Java | 25 (compatível com 21+) |
| **Framework Base** | Spring Boot | 4.1.1 (Spring WebMvc, Spring Data JPA, Spring Validation) |
| **Banco de Dados Principal** | PostgreSQL | 15 (Alpine via Docker) |
| **Banco de Dados em Memória** | H2 Database | Perfil `local` para testes e desenvolvimento rápido |
| **ORM / Persistência** | Hibernate ORM | 7.4.x |
| **SDK de Pagamentos** | Mercado Pago Java SDK | 2.1.21 (`com.mercadopago:sdk-java`) |
| **Automação de Build** | GNU Make | `Makefile` cross-platform (Windows / Linux / macOS) |
| **Conteinerização** | Docker & Docker Compose | Multi-stage build com `eclipse-temurin:25` |
| **Orquestração de Contêineres** | Kubernetes & Kustomize | `k8s/base` + `k8s/overlays/aws` (HPA, Deployment, Service) |
| **Registry de Imagens** | Amazon ECR | Repositório `oficina-billing-service` (tags imutáveis) |
| **Deploy & CI/CD** | GitHub Actions | Pipelines independentes de CI (`maven.yml`) e CD (`deploy.yml`) |
| **Monitoramento & Saúde** | Spring Boot Actuator | `/actuator/health` (liveness, readiness e startup probes) |
| **Documentação da API** | SpringDoc OpenAPI | 3.1.0 (Swagger UI v3) |
| **Produtividade** | Lombok | 1.18.x |
| **Testes Unitários / Mock** | JUnit 5 & Mockito | 41 testes automatizados via Maven Surefire |
| **Testes E2E / API Client** | Bruno | Coleção completa de requisições em `src/test/bruno/` |

---

## 🏛️ Arquitetura

O microsserviço segue os princípios da **Clean Architecture (Arquitetura Limpa)** e **Ports & Adapters (Arquitetura Hexagonal)**, com as dependências fluindo sempre de fora para dentro:

```
[ Adapters: Web / Persistence / MercadoPago ] ──> [ Application: Use Cases / Gateways ] ──> [ Domain: Entidades / Regras de Negócio ]
```

* **`domain/`**: Entidades de negócio puras (`Cobranca`, `Orcamento`, `ItemOrcamento`) e regras invariantes. Não possui dependências do framework Spring nem de infraestrutura externa.
* **`application/`**: Casos de uso (`CriarCobrancaUseCase`, `ProcessarWebhookMercadoPagoUseCase`, `GerarOrcamentoUseCase`, `AprovarOrcamentoUseCase`, etc.) e interfaces de saída (Gateways).
* **`adapters/`**:
  * `web/`: Controladores REST (`CobrancaController`, `OrcamentoController`, `MercadoPagoWebhookController`), DTOs e tratamento global de exceções ([GlobalExceptionHandler](src/main/java/com/mecanica/oficina_billing_service/adapters/web/GlobalExceptionHandler.java)).
  * `persistence/`: Repositórios Spring Data JPA e gateways de persistência.
  * `mercadopago/`: Integração oficial com a API do Mercado Pago via SDK e validador de assinatura criptográfica HMAC-SHA256 ([MercadoPagoWebhookValidator](src/main/java/com/mecanica/oficina_billing_service/adapters/mercadopago/MercadoPagoWebhookValidator.java)).
  * `config/`: Propriedades tipadas e configurações de ambiente.

### 🌐 Integração no Ecossistema de Microsserviços e AWS

O microsserviço de faturamento opera como um serviço autônomo dentro do ecossistema distribuído da oficina na AWS:

```mermaid
graph TD
    Client["📱 Cliente / Frontend"] --> AGW["🚪 AWS API Gateway (oficina-auth-gateway)\n- POST /auth (autenticação JWT)\n- ANY /billing/* (JWT Authorizer)\n- POST /webhooks/mercadopago (Público HMAC)"]
    
    subgraph EKS["🖥️ Cluster EKS (eks-oficina-terraform)"]
        SVC["🌐 Service: oficina-billing\n(Type: LoadBalancer / Porta 8082)"]
        HPA["📈 HPA\n(1-5 réplicas | 70% CPU / 80% RAM)"]
        POD["📦 Pods: oficina-billing-service\n- Java 25 / Spring Boot 4.1.1\n- Probes: /actuator/health"]
        SVC --> POD
        HPA -.->|Controla| POD
    end

    subgraph Dados["🗄️ Camada de Dados (oficina-infra-database)"]
        RDS[("🐘 AWS RDS PostgreSQL\nDatabase: billing_db")]
    end

    subgraph Provedor Pagamento["💳 Mercado Pago"]
        MP_API["API Checkout Pro (Headless)"]
        MP_WH["Notificações Webhook (HMAC-SHA256)"]
    end

    AGW -->|HTTP Proxy| SVC
    POD -->|JDBC Connection| RDS
    POD -->|Cria Preferência| MP_API
    MP_WH -->|Callback de Pagamento| AGW

    style POD fill:#22c55e,color:#fff
    style AGW fill:#3b82f6,color:#fff
    style RDS fill:#6366f1,color:#fff
    style MP_API fill:#06b6d4,color:#fff
    style MP_WH fill:#06b6d4,color:#fff
```

#### Papel dos Repositórios Integrados:
* **[`oficina-infra-cluster`](../oficina-infra-cluster)**: Provisiona a VPC, Subnets, o cluster EKS (`eks-oficina-terraform`) e o repositório ECR (`oficina-billing-service`).
* **[`oficina-infra-database`](../oficina-infra-database)**: Provisiona a instância PostgreSQL compartilhada no RDS; as tabelas do billing ficam isoladas na base `billing_db`.
* **[`oficina-auth-gateway`](../oficina-auth-gateway)**: Ponto único de entrada com rate limiting por IP, autenticação JWT para endpoints de negócios (`/billing/*`) e encaminhamento do webhook público do Mercado Pago (`/webhooks/mercadopago`).
* **[`oficina-billing-service`](.)**: Repositório do microsserviço com pipeline de CD independente, aplicando manifestos Kustomize diretamente no cluster EKS.

---

## 📚 Documentação Adicional

Para instruções operacionais e detalhes do ciclo de vida na nuvem, consulte a pasta [`docs/`](docs/):

* 🚀 **[Guia de Deploy e Destruição na AWS (EKS + RDS + CD)](docs/deploy-aws.md):** Manual completo com o passo a passo de subida das esteiras de CI/CD, configuração de credenciais no Learner Lab, testes de ponta a ponta na nuvem e o **roteiro obrigatório de destruição da infraestrutura** para evitar consumo indevido de créditos.
* 📨 **[Mensageria (SNS + SQS)](docs/mensageria.md):** Outbox, consumidor idempotente, DLQ e LocalStack para a Saga coreografada.

---

## 🗄️ Banco de Dados e Justificativa da Escolha

O banco de dados relacional **PostgreSQL 15** foi selecionado como banco principal do serviço pelos seguintes motivos técnicos:

1. **Garantias ACID e Consistência Transacional**:
   * Por se tratar de um serviço com impacto financeiro (faturamento de ordens de serviço, cálculo de orçamentos e conciliação de pagamentos), a consistência estrita dos dados é imprescindível. O PostgreSQL assegura transações atômicas e isoladas, mitigando o risco de cobrança duplicada e concorrência na transição de estados.
2. **Precisão Monetária Exata**:
   * Uso nativo do tipo `numeric(12,2)` para cálculo e persistência de valores financeiros, evitando erros clássicos de ponto flutuante (`float`/`double`).
3. **Integridade Referencial e Constraints**:
   * Garantia de integridade com Foreign Keys entre orçamentos e itens, além de restrições de unicidade em `preference_id` e `referencia` para rastreabilidade fiscal.
4. **Tipos Nativos e Auditoria**:
   * Uso nativo de `UUID` como identificador público das cobranças e orçamentos, e timestamps com fuso horário (`timestamp with time zone`) para auditoria de criação e liquidação.
5. **Banco H2 Auxiliar (In-Memory)**:
   * Disponibilizado sob o perfil `local` para acelerar o ciclo de desenvolvimento e viabilizar a execução dos 40 testes automatizados em pipelines de CI/CD sem a necessidade de instanciar containers externos.

---

## 💳 Integração Mercado Pago Checkout Pro (API Headless)

O serviço é **100% headless** (sem interface gráfica ou assets estáticos no backend). O fluxo de pagamento opera via **redirecionamento nativo**:

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Usuário Final
    participant Front as Frontend / App / WhatsApp
    participant Billing as oficina-billing-service
    participant MP as Mercado Pago

    Cliente->>Front: Solicita pagamento da Ordem de Serviço
    Front->>Billing: POST /cobrancas (pedidoId, valor, returnUrl...)
    Billing->>MP: Cria preferência Checkout Pro
    MP-->>Billing: Retorna preference com linkPagamento (init_point)
    Billing-->>Front: 201 Created { cobrancaId, linkPagamento, referencia, status }
    Front->>Cliente: Redireciona para linkPagamento
    Cliente->>MP: Conclui pagamento (Pix, Cartão ou Boleto)
    MP-->>Cliente: Redireciona para a returnUrl informada
    MP->>Billing: POST /webhooks/mercadopago (Webhook assinado HMAC-SHA256)
    Billing->>Billing: Valida autenticidade criptográfica e atualiza status para PAGO
```

### Exemplo de Requisição: `POST /cobrancas`
```http
POST /cobrancas HTTP/1.1
Host: localhost:8082
Content-Type: application/json

{
  "pedidoId": "OS-2026-001",
  "descricao": "Revisão geral e troca de pastilhas de freio",
  "valor": 250.00,
  "payerEmail": "cliente@email.com",
  "payerName": "Carlos Silva",
  "payerCpf": "12345678901",
  "returnUrl": "https://meufrontend.com.br/pedidos/OS-2026-001/retorno"
}
```

### Exemplo de Resposta (`201 Created`):
```json
{
  "cobrancaId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "pedidoId": "OS-2026-001",
  "valor": 250.00,
  "linkPagamento": "https://sandbox.mercadopago.com.br/checkout/v1/redirect?pref_id=3784984170-...",
  "referencia": "oficina-billing:dev:OS-2026-001",
  "status": "PENDENTE"
}
```

---

## ✅ Testes e qualidade

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=GrupoPosTech-FIAP_oficina-billing-service&metric=alert_status)](https://sonarcloud.io/summary/overall?id=GrupoPosTech-FIAP_oficina-billing-service) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GrupoPosTech-FIAP_oficina-billing-service&metric=coverage)](https://sonarcloud.io/component_measures?id=GrupoPosTech-FIAP_oficina-billing-service&metric=coverage)

```bash
./mvnw clean verify   # testes + relatório e gate do JaCoCo
```

- **Cobertura:** o JaCoCo reprova o `verify` abaixo de **80% de linhas**. Hoje: **85,3%** (94 testes).
  Relatório em `target/site/jacoco/index.html`; o CI escreve o percentual no resumo de cada execução.
- **Qualidade:** o CI ([`maven.yml`](.github/workflows/maven.yml)) analisa o código no
  [SonarCloud](https://sonarcloud.io/summary/overall?id=GrupoPosTech-FIAP_oficina-billing-service) e o build falha se o
  quality gate reprovar. A análise roda quando o secret `SONAR_TOKEN` existe na organização.

## 🚀 Como Executar

### Pré-requisitos
* **Docker e Docker Compose** instalados (ou ferramenta `make`).
* **Java 25** (ou JDK 21+) caso deseje compilar ou executar fora do container.

---

### Execução via `make` (Padrão Recomendado)

O projeto conta com um [`Makefile`](Makefile) padronizado, compatível com o ecossistema da oficina:

| Comando | Descrição |
| :--- | :--- |
| `make start` | Limpa volumes antigos, compila a imagem Docker e sobe o `billing-app` e `billing-db` com logs. |
| `make dev` | Sobe apenas o PostgreSQL via Docker e aguarda ficar pronto (`pg_isready`), liberando o terminal para rodar o backend localmente na IDE. |
| `make debug` | Sobe o PostgreSQL e inicia o Spring Boot local com suporte a **Remote JVM Debug na porta 5006** (Attach na IDE). |
| `make up` | Sobe os containers em segundo plano (`docker compose up -d`). |
| `make down` | Para os containers e remove volumes criados. |
| `make build` | Compila o projeto e gera o arquivo `.jar` pulando testes. |
| `make logs` | Exibe os logs em tempo real da aplicação (`billing-app`). |
| `make logs-db` | Exibe os logs em tempo real do banco de dados (`billing-db`). |
| `make test` | Executa todos os testes unitários e de integração via Maven. |
| `make test-e2e` | Executa os testes de API de ponta a ponta com o Bruno CLI. |
| `make clean` | Executa o clean do Maven e remove imagens/volumes locais do Docker. |
| `make info` | Exibe no terminal as portas e URLs de acesso da aplicação e do banco. |

---

### Execução Manual sem `make`

#### Opção 1: Via Docker Compose Direto
```bash
docker compose up --build
```

#### Opção 2: Banco em Memória H2 (Desenvolvimento Rápido sem Docker)
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
*(O perfil `local` com H2 é o default quando nenhuma configuração de PostgreSQL externa é informada).*

#### Opção 3: PostgreSQL via Docker + Spring Boot Local
```powershell
# 1. Sobe apenas o banco
docker compose up -d billing-db

# 2. Inicia o serviço apontando para o banco
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=default"
```

---

### Endpoints e Interfaces Úteis

* **Swagger UI (Documentação Interativa OpenAPI):** [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
* **OpenAPI Docs (JSON):** [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs)
* **H2 Web Console:** [http://localhost:8082/h2-console](http://localhost:8082/h2-console)
  * *JDBC URL:* `jdbc:h2:mem:billing_db`
  * *User Name:* `sa`
  * *Password:* *(deixar em branco)*
* **Spring Boot Actuator Health:** [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)
* **Spring Boot Actuator Info:** [http://localhost:8082/actuator/info](http://localhost:8082/actuator/info)

---

### Variáveis de Ambiente Suportadas

É possível configurar as variáveis via arquivo [`.env`](.env.example) ou variáveis de ambiente do sistema:

| Variável | Descrição | Valor Padrão |
| :--- | :--- | :--- |
| `APP_PORT` | Porta HTTP da aplicação | `8082` |
| `DB_PORT` | Porta mapeada do PostgreSQL no host | `5432` |
| `POSTGRES_DB` | Nome do banco de dados | `billing_db` |
| `POSTGRES_USER` | Usuário do PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | `123` |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo do Spring Boot | `local` (ou `default` no Docker) |
| `MP_ACCESS_TOKEN` | Credencial privada de API do Mercado Pago | Token de teste pré-configurado |
| `MP_WEBHOOK_SECRET` | Chave secreta para validação criptográfica HMAC | Vazia (modo permissivo em dev) |
| `MP_WEBHOOK_PUBLIC_URL` | URL pública onde o Mercado Pago enviará notificações | `http://localhost:8082/webhooks/mercadopago` |
| `MP_BACK_URL_SUCCESS` | URL padrão de redirecionamento pós-aprovação | `https://oficina.com.br/pagamentos/sucesso` |

---

## 📊 Observabilidade

A imagem roda com o agente do **OpenTelemetry** (como na app-api, ADR 002), que exporta traces, métricas e logs por OTLP
para o **New Relic** como `service.name=oficina-billing-service` (`service.namespace=oficina`). O agente captura HTTP, banco, SNS/SQS
e o relay do outbox sem dependência no código; os logs da Saga trazem `saga_id` e `event_type` para acompanhar uma OS
entre os serviços. Consultas NRQL e alertas: [monitoramento e observabilidade](https://github.com/GrupoPosTech-FIAP/oficina-app-api/blob/main/docs/monitoramento-e-observabilidade.md).

- **AWS:** as variáveis `OTEL_*` ficam no `k8s/base/configmap.yaml`; o `deploy.yml` monta a license key a partir do
  secret `NEW_RELIC_LICENSE_KEY` e desliga o agente (`OTEL_SDK_DISABLED=true`) quando ele não existe.
- **Local:** no `docker-compose` o agente vem desligado; para ligar, defina `NEW_RELIC_LICENSE_KEY` e
  `OTEL_SDK_DISABLED=false` no `.env`.

## ☁️ Deploy e Infraestrutura Kubernetes (K8s & CI/CD Independente)

O `oficina-billing-service` possui ciclo de vida e **Continuous Deployment (CD) 100% desacoplado**, utilizando Kustomize para gerenciar os manifestos declarativos do Kubernetes e GitHub Actions para a automação.

> 📖 **Guia Operacional Completo:** Para o passo a passo detalhado de implantação na AWS, execução de testes remotos e o procedimento obrigatório de destruição para economia de créditos no Learner Lab, consulte [`docs/deploy-aws.md`](docs/deploy-aws.md).

### 📁 Estrutura de Manifestos Kustomize (`k8s/`)

```
k8s/
├── base/
│   ├── kustomization.yaml           # Orquestrador base de recursos
│   ├── app-deployment.yaml          # Pods, portas, recursos e probes
│   ├── app-service.yaml             # Service ClusterIP (porta 8082)
│   ├── app-hpa.yaml                 # Autoscaler horizontal (1-5 réplicas)
│   └── configmap.yaml               # Variáveis de ambiente não sensíveis
└── overlays/
    └── aws/
        ├── kustomization.yaml       # Aplica patches e substitui imagem do ECR
        ├── service-loadbalancer.yaml# Promove o Service a LoadBalancer na AWS
        ├── configmap-rds.yaml       # Injeta dinamicamente o endpoint do RDS
        ├── secret.example.yaml      # Modelo documental de secrets
        └── secret.yaml              # Gerado efemeramente na pipeline (no .gitignore)
```

### ⚙️ Recursos Kubernetes Gerenciados:
1. **Deployment (`app-deployment.yaml`)**:
   * Executa a imagem imutável publicada no Amazon ECR (`oficina-billing-service`).
   * Configura limites e reservas de recursos: `requests: { cpu: 250m, memory: 512Mi }` e `limits: { cpu: 500m, memory: 768Mi }`.
   * **Probes de Saúde (Actuator)**:
     * `startupProbe`: aguarda a inicialização do Spring Boot (até 150s) sem matar o pod antecipadamente.
     * `readinessProbe`: verifica `/actuator/health` antes de enviar tráfego do Service.
     * `livenessProbe`: verifica a estabilidade contínua da aplicação.
2. **Service (`app-service.yaml` + overlay `service-loadbalancer.yaml`)**:
   * Expõe a porta `8082` da aplicação.
   * Na AWS, o patch Kustomize converte o tipo para `LoadBalancer`, disponibilizando um endereço ELB para integração direta e consumo pelo API Gateway.
3. **Horizontal Pod Autoscaler (`app-hpa.yaml`)**:
   * Escala automaticamente a quantidade de pods entre **1 (mínimo)** e **5 (máximo)** baseado na utilização média de 70% de CPU ou 80% de memória.
4. **ConfigMap & Secret (`configmap.yaml`, `secret.yaml`)**:
   * Separação estrita de configuração (`billing-config`) e segredos (`billing-secret`).
   * O arquivo `secret.yaml` é gerado efemeramente durante a execução da esteira de CD e é estritamente ignorado no Git via [`.gitignore`](.gitignore).

---

### 🔁 Pipeline de CD via GitHub Actions ([`.github/workflows/deploy.yml`](.github/workflows/deploy.yml))

A publicação no cluster EKS da AWS é automatizada e independente dos demais microsserviços:

* **Gatilhos de Execução:**
  * **Manual (`workflow_dispatch`)**: Requer o input `rds_endpoint` (obtido com `terraform output -raw DB_Endpoint` no repositório `oficina-infra-database`). Essencial para suportar a rotação de instâncias e credenciais temporárias do AWS Learner Lab (~4h).
  * **Automático**: Ao publicar uma tag Git versionada (ex: `v1.0.0`).

* **Etapas da Pipeline de CD:**
  1. **Checkout & Versionamento**: Determina a tag da imagem (tag Git ou `GITHUB_SHA` curto).
  2. **Autenticação AWS & Login ECR**: Conecta-se à AWS e realiza login no registro de contêineres.
  3. **Build & Push Docker**: Constrói a imagem baseada em Java 25 e faz push para o ECR `oficina-billing-service`.
  4. **Kubeconfig EKS**: Configura as credenciais de acesso ao cluster `eks-oficina-terraform`.
  5. **Injeção de Secrets e Configuração**:
     * Gera o `k8s/overlays/aws/secret.yaml` em Base64 com os secrets do GitHub (`RDS_PASSWORD`, `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET`, `MP_PUBLIC_KEY`).
     * Atualiza dinamicamente o `kustomization.yaml` com o registry e tag da imagem recém-construída.
     * Injeta o `rds_endpoint` informado no `configmap-rds.yaml`.
  6. **Deploy & Rollout**: Executa `kubectl apply -k k8s/overlays/aws` e valida a estabilização com `kubectl rollout status deploy/oficina-billing`.

#### 🔐 Secrets Necessários no Repositório GitHub

Para o funcionamento do CD, cadastre em **Settings > Secrets and variables > Actions**:

| Secret | Descrição | Origem |
| :--- | :--- | :--- |
| `AWS_ACCESS_KEY_ID` | Chave de acesso AWS | AWS Learner Lab / IAM |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta AWS | AWS Learner Lab / IAM |
| `AWS_SESSION_TOKEN` | Token temporário de sessão | AWS Learner Lab |
| `RDS_PASSWORD` | Senha master do PostgreSQL | Mesma senha de `oficina-infra-database` |
| `MP_ACCESS_TOKEN` | Token privado do Mercado Pago | Painel do Desenvolvedor Mercado Pago |
| `MP_WEBHOOK_SECRET` | Chave HMAC de assinatura de webhook | Configuração de Webhooks Mercado Pago |
| `MP_PUBLIC_KEY` | Chave pública do Mercado Pago | Painel do Desenvolvedor Mercado Pago |
| `NEW_RELIC_LICENSE_KEY` | License key de ingestão do New Relic (opcional; sem ela o agente fica desligado) | New Relic > API Keys |

#### 🧪 Validação Local dos Manifestos (sem aplicar no cluster):
```bash
# Renderiza todos os objetos combinados pelo Kustomize para validação visual
kubectl kustomize k8s/overlays/aws
```

---

## 🧪 Como Executar os Testes

O projeto adota uma estratégia de testes em duas camadas: **Testes Automatizados (Unitários e de Integração)** e **Testes de Contrato E2E**.

### 1. Testes Automatizados (Maven)

A suíte possui **41 testes automatizados** cobrindo entidades de domínio, orçamentos, criação de preferências no Checkout Pro com idempotência, tratamento semântico de erros HTTP e validação criptográfica HMAC-SHA256 de webhooks.

* **Via Make:**
  ```bash
  make test
  ```
* **Via Maven Wrapper:**
  ```powershell
  # Windows
  .\mvnw.cmd clean test

  # Linux / macOS
  ./mvnw clean test
  ```

* **Executar classe de teste específica:**
  ```powershell
  # Validação criptográfica HMAC-SHA256 e replay attacks em Webhooks
  .\mvnw.cmd test -Dtest=MercadoPagoWebhookValidatorTest

  # Controller REST de Cobranças
  .\mvnw.cmd test -Dtest=CobrancaControllerTest

  # Processamento de Webhooks e reconciliação
  .\mvnw.cmd test -Dtest=ProcessarWebhookMercadoPagoUseCaseTest

  # Gateway com SDK do Mercado Pago
  .\mvnw.cmd test -Dtest=MercadoPagoClientGatewayTest
  ```

---

### 2. Testes E2E com Bruno (API Client)

A coleção completa de requisições está disponível em [`src/test/bruno/`](src/test/bruno/).

#### Suíte Sequencial do Caminho Feliz (`caminho-feliz/`):
1. `01 - Gerar Orcamento`: Cria um orçamento com serviços e insumos.
2. `02 - Consultar Orcamento por ID`: Valida o orçamento gravado.
3. `03 - Consultar Orcamento por OS`: Valida a busca vinculada à Ordem de Serviço.
4. `04 - Enviar Orcamento ao Cliente`: Transição de status para `ENVIADO`.
5. `05 - Aprovar Orcamento`: Transição de status para `APROVADO`.
6. `06 - Criar Cobranca Mercado Pago`: Cria a cobrança e extrai a URL do Checkout Pro.
7. `07 - Consultar Cobranca por ID`: Consulta o status `PENDENTE` da cobrança.
8. `08 - Consultar Cobranca por Pedido OS`: Consulta a cobrança vinculada ao pedido.
9. `09 - Simular Webhook Mercado Pago`: Dispara evento de pagamento aprovado.
10. `10 - Listar Orcamentos` / `11 - Listar Cobrancas`: Validações de listagem.

#### Executando via Bruno Desktop:
1. Abra o aplicativo [Bruno](https://www.usebruno.com/).
2. Selecione **Open Collection** e selecione a pasta `src/test/bruno/`.
3. Escolha o ambiente **`local`** no canto superior direito.
4. Clique com o botão direito na pasta **`caminho-feliz`** e selecione **Run Folder**.

#### Executando via Linha de Comando (Bruno CLI):
```bash
# Via Make:
make test-e2e

# Ou diretamente via npx (a partir da raiz da coleção):
cd src/test/bruno && npx @usebruno/cli run caminho-feliz --env local
```

---

## 📄 Licença

Distribuído sob a licença [MIT](LICENSE).
