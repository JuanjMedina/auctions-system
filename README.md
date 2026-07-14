# Auctions System

Sistema de subastas en tiempo real construido con **arquitectura hexagonal** sobre Spring Boot. Los compradores pujan con fondos reservados en una billetera virtual; las subastas se activan, extienden y cierran automáticamente, y el ganador paga al vendedor de forma atómica.

**Stack:** Java 21 · Spring Boot 4.0.6 · PostgreSQL 16 · Flyway · Gradle multi-módulo · JWT · Testcontainers

## Arquitectura

Hexagonal (ports & adapters) con las capas separadas en módulos Gradle — la dependencia entre capas la impone el compilador:

```
domain/                  Agregados y reglas de negocio puras (Auction, Bid, Wallet, User...)
usecase/                 Casos de uso; orquestan dominio a través de puertos
adapter/
  in/web/                Controllers REST, seguridad JWT
  in/scheduler/          Jobs: activar/cerrar subastas, poller del outbox
  out/persistence/       JPA + Spring Data (implementa los puertos de dominio)
  out/messaging/         Publicación de eventos de integración (in-process, swappable a broker)
  exception/             Mapeo de excepciones de dominio → HTTP
app/spring/              Arranque, configuración, migraciones Flyway
```

Reglas clave del diseño:

- `domain` no conoce Spring ni JPA. Los agregados exponen comportamiento (`auction.placeBid()`, `wallet.reserve()`) y validan sus invariantes.
- Los puertos (`AuctionRepository`, `WalletRepository`, `TokenGenerator`...) viven en `domain`; los adaptadores los implementan.
- **Optimistic locking** (`@Version` + retry con backoff) protege pujas concurrentes.
- **Patrón outbox** para eventos de dominio (`BID_PLACED`, `AUCTION_AWARDED`...): un poller los publica por el puerto `EventPublisher` (transacción por evento, reintentos con máximo y registro del último error).
- Fondos de pujas: se **reservan** al pujar, se **liberan** al ser superado y se **cobran** al adjudicar — siempre en la misma transacción.

## Quick start

Requisitos: JDK 21 y Docker.

```bash
docker compose up -d                  # PostgreSQL en localhost:5433
./gradlew :app:spring:bootRun         # API en http://localhost:8080
```

Flyway crea el esquema y carga datos de desarrollo (`V8__seed_dev_data.sql`): usuarios, categorías y subastas de prueba. Todos los usuarios seed usan la contraseña `Password123!`.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Colección Postman:** [`auctions-system.postman_collection.json`](auctions-system.postman_collection.json)

### Variables de entorno (opcionales en dev)

| Variable | Default | Descripción |
|---|---|---|
| `JWT_SECRET` | secreto de dev | Clave HMAC (mín. 32 chars) |
| `JWT_ACCESS_EXPIRATION_MS` | 86400000 (24 h) | Vida del access token |
| `JWT_REFRESH_EXPIRATION_MS` | 604800000 (7 d) | Vida del refresh token |

## API

Autenticación: `Authorization: Bearer <token>` (obtenido en `/auth/login`). Los `GET` de subastas y categorías son públicos.

| Recurso | Endpoints |
|---|---|
| Auth | `POST /auth/register` · `/auth/login` · `/auth/refresh` |
| Subastas | `POST /auctions` · `GET /auctions` · `GET /auctions/{id}` · `PATCH /auctions/{id}/publish` · `POST /auctions/{id}/close` · `DELETE /auctions/{id}` |
| Pujas | `POST /auctions/{id}/bids` · `GET /auctions/{id}/bids` · `DELETE /auctions/{id}/bids/{bidId}` |
| Billetera | `GET /wallets/me` · `POST /wallets/me/deposit` · `/me/withdraw` · `GET /me/transactions` |
| Categorías | `GET /categories` · `GET /categories/{id}` · `POST /categories` |
| Perfil | `GET/PATCH /users/me` · `POST /users/me/change-password` · `GET /users/me/bids` · `/me/auctions` |
| Favoritos | `GET /watchlist` · `POST /watchlist/{auctionId}` · `DELETE /watchlist/{auctionId}` |

### Ciclo de vida de una subasta

```
DRAFT → (publish) → SCHEDULED → (scheduler) → ACTIVE ⇄ EXTENDED
                                                 │
                                    (scheduler/close) → AWARDED | FAILED
DRAFT/SCHEDULED → (cancel) → CANCELLED
```

Los schedulers corren cada 30 s: activan subastas programadas y cierran las expiradas (adjudicando al ganador si se alcanzó el precio de reserva).

## Tests y cobertura

```bash
./gradlew test                        # unitarios + integración (integración requiere Docker)
./gradlew codeCoverageReport          # JaCoCo agregado por capa de arquitectura
# → build/reports/jacoco/codeCoverageReport/html/index.html
```

- **Unitarios** por capa (dominio y casos de uso con Mockito).
- **Integración** en `app/spring` con Testcontainers (PostgreSQL real).
- Formato con Spotless (Google Java Format), aplicado también por hook de pre-commit.

## Roadmap

- [x] Outbox completo con publisher (in-process vía eventos de Spring)
- [ ] Sustituir el publisher in-process por un broker (Kafka/RabbitMQ)
- [ ] CI (build + tests en cada push)
- [ ] Perfiles `dev`/`prod` y endurecimiento de JWT (revocación, tokens cortos)
- [ ] Observabilidad (métricas y trazas)
- [ ] Extracción gradual a microservicios usando los puertos como costuras
