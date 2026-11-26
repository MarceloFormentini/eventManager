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

| Recurso | Listagem paginada | Filtro paginado | Demais operações |
| --- | --- | --- | --- |
| Eventos | `GET /api/v1/events` | `GET /api/v1/events/location/{locationId}` | `GET /api/v1/events/{id}`, `POST /api/v1/events`, `PUT /api/v1/events/{id}`, `PATCH /api/v1/events/{id}/status`, `DELETE /api/v1/events/{id}` |
| Instituições | `GET /api/v1/institutions` | `GET /api/v1/institutions/type/{typeInstitutionId}` | `GET /api/v1/institutions/{id}`, `POST /api/v1/institutions`, `PUT /api/v1/institutions/{id}`, `DELETE /api/v1/institutions/{id}` |
| Locais | `GET /api/v1/locations` | `GET /api/v1/locations/institution/{institutionId}` | `GET /api/v1/locations/{id}`, `POST /api/v1/locations`, `PUT /api/v1/locations/{id}`, `DELETE /api/v1/locations/{id}` |
| Tipos de instituição | `GET /api/v1/institution-types` | — | `GET /api/v1/institution-types/{id}`, `POST /api/v1/institution-types`, `PUT /api/v1/institution-types/{id}`, `DELETE /api/v1/institution-types/{id}` |

Todas as listagens aceitam `page=0`, `size=20`, `sort` e `direction` (`asc` ou
`desc`). Tamanho máximo: 100. Campos de ordenação: eventos (`id`, `createdAt`,
`title`, `startDate`, `endDate`, `status`); instituições (`id`, `createdAt`,
`name`); locais (`id`, `createdAt`, `name`, `capacity`); tipos (`id`,
`createdAt`, `name`). Os campos padrão são `createdAt` e direção `desc`.

As listagens retornam `content`, `page`, `size`, `totalElements`, `totalPages`,
`first` e `last`. A página padrão é `0` e o tamanho padrão é `20`. A API rejeita
parâmetros de paginação e ordenação inválidos com HTTP `400`.

Eventos em `DRAFT` e `PUBLISHED` ocupam o horário. Eventos `CANCELLED` e
`COMPLETED` liberam o horário. Transições permitidas: `DRAFT` para `PUBLISHED`
ou `CANCELLED`; `PUBLISHED` para `CANCELLED` ou `COMPLETED`. Eventos em estado
terminal (`CANCELLED` ou `COMPLETED`) não podem ser alterados; eventos publicados
podem ter seus dados editados, mas não podem voltar a rascunho. Erros de validação
retornam `400`, conflitos de agenda e integridade retornam `409`, recursos
inexistentes retornam `404`, e exclusões bem-sucedidas retornam `204`.

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
