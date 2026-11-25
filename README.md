# Event Manager

Aplicação de gestão de tipos de instituição, instituições, locais e eventos.
O frontend Next.js consome a API REST Spring Boot, persistida em PostgreSQL e
integrada ao RabbitMQ pelo Transactional Outbox.

## Tecnologias

- Java 21
- Spring Boot
- PostgreSQL 17
- Flyway
- RabbitMQ
- Maven
- Testcontainers

## Executar com Docker

```sh
docker compose up --build
```

Abra o frontend em `http://localhost:3000` e a API em `http://localhost:8081`
(altere `BACKEND_PORT` se essa porta já estiver em uso). PostgreSQL e RabbitMQ
ficam disponíveis apenas na rede interna do Compose.

O frontend encaminha as chamadas por `/api/backend/*` para o serviço backend
interno; assim, o navegador não precisa resolver nomes da rede Docker.

Para desenvolvimento local sem Docker para a aplicação, use
`docker compose -f backend/compose.yaml up -d`, execute `.\mvnw.cmd spring-boot:run`
no diretório `backend` e `npm ci && npm run dev` em `frontend`.

Endpoints principais:

- `GET /api/v1/events`
- `GET /api/v1/events?page=0&size=20&sort=createdAt&direction=desc`
- `POST /api/v1/events`
- `PUT /api/v1/events/{id}`
- `PATCH /api/v1/events/{id}/status`
- `DELETE /api/v1/events/{id}`

Actuator: `http://localhost:8080/actuator/health`

## Testes

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd verify
```

Os testes de integração usam Testcontainers para PostgreSQL e RabbitMQ.

## Arquitetura do Outbox

Evento e `OutboxEvent` são persistidos na mesma transação PostgreSQL. O
`OutboxDispatcher` bloqueia registros pendentes, publica no RabbitMQ e so altera
o status para `PUBLISHED` após confirmação positiva e ausência de retorno.

Falhas usam retry exponencial com limite configurável. O consumidor registra
eventos processados no PostgreSQL, garantindo idempotência diante de entregas
duplicadas.

## Campos persistidos

- **Tipo de instituição:** nome e descrição.
- **Instituição:** nome, tipo, endereço, telefone e e-mail.
- **Local:** nome, instituição, capacidade e descrição.
- **Evento:** título, descrição, local, início, fim e status (`DRAFT`,
  `PUBLISHED`, `CANCELLED` ou `COMPLETED`).

As migrations Flyway aplicam automaticamente os campos adicionais existentes
na interface. `GET /actuator/health` verifica a saúde do backend.
