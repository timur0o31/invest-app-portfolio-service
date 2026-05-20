# Portfolio Service Testing

This guide checks the MVP portfolio flow:

- account creation
- balance reset
- deposit
- market buy order
- portfolio summary
- positions
- operations
- orders
- analytics

## 1. Fast Compile Check

From `invest-app-portfolio-service`:

```bash
./gradlew test
```

Expected result:

```text
BUILD SUCCESSFUL
```

There are no test classes yet, so this mostly verifies compilation and resource processing.

## 2. Required Local Services

The full HTTP smoke test needs:

| Service | URL |
| --- | --- |
| auth-service | `http://127.0.0.1:8081` |
| quotes-service | `http://127.0.0.1:8082` |
| portfolio-service | `http://127.0.0.1:8083` |
| Valkey | `redis://127.0.0.1:6379` |
| ClickHouse | `http://127.0.0.1:8123` |
| auth Postgres | `127.0.0.1:5436` |
| portfolio Postgres | `127.0.0.1:5437` |

All backend services must use the same JWT settings:

```bash
JWT_SECRET=dev-secret
JWT_ISSUER=invest-app
```

## 3. Start Infrastructure

Start market-data infrastructure:

```bash
cd ../mobile-apps-development/metrics_collector
docker compose up -d
```

Start auth Postgres only:

```bash
cd ../invest-app-auth-service
docker compose -f docker-compose.infra.yml up -d postgres
```

Start portfolio Postgres:

```bash
cd ../invest-app-portfolio-service
docker compose -f docker-compose.infra.yml up -d postgres
```

Do not start another Valkey on port `6379` if `metrics_collector-valkey` is already running.

## 4. Start Backend Services

Terminal 1: auth-service

```bash
cd ../invest-app-auth-service
JDBC_DATABASE_URL=jdbc:postgresql://127.0.0.1:5436/auth-db \
DB_USER=auth-user \
DB_PASSWORD=auth-password \
JWT_SECRET=dev-secret \
JWT_ISSUER=invest-app \
VALKEY_URL=redis://127.0.0.1:6379 \
JAVA_TOOL_OPTIONS="-Dktor.deployment.port=8081" \
./gradlew run
```

Terminal 2: quotes-service

```bash
cd ../invest-app-quotes-service
JWT_SECRET=dev-secret \
JWT_ISSUER=invest-app \
VALKEY_URL=redis://127.0.0.1:6379 \
VALKEY_QUOTES_CHANNEL=quotes.ticks \
CLICKHOUSE_ENDPOINT=http://127.0.0.1:8123 \
CLICKHOUSE_DATABASE=default \
CLICKHOUSE_TABLE=raw_ticks \
CLICKHOUSE_USERNAME=collector \
CLICKHOUSE_PASSWORD=collector_pass \
JAVA_TOOL_OPTIONS="-Dktor.deployment.port=8082" \
./gradlew run
```

Terminal 3: portfolio-service

```bash
cd ../invest-app-portfolio-service
JDBC_DATABASE_URL=jdbc:postgresql://127.0.0.1:5437/portfolio-db \
DB_USER=portfolio-user \
DB_PASSWORD=portfolio-password \
JWT_SECRET=dev-secret \
JWT_ISSUER=invest-app \
QUOTES_BASE_URL=http://127.0.0.1:8082 \
JAVA_TOOL_OPTIONS="-Dktor.deployment.port=8083" \
./gradlew run
```

## 5. Create User And Token

```bash
EMAIL="portfolio-smoke-$(date +%s)@test.local"
PASSWORD="Password123"

REGISTER=$(curl -s -X POST http://127.0.0.1:8081/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

ACCESS=$(echo "$REGISTER" | jq -r .accessToken)
echo "$ACCESS"
```

The token must not be `null`.

## 6. Seed A Predictable Quote

Orders use quotes-service to get the execution price. Publish a known quote for `instrumentId=1002`:

```bash
docker exec -i metrics_collector-valkey valkey-cli PUBLISH quotes.ticks \
'{"seq_no":1,"ts_unix_ms":1778605586199,"instrument_id":1002,"price_kopecks":100000,"qty_lots":1,"side":1}'
```

Check that quotes-service sees it:

```bash
curl -s "http://127.0.0.1:8082/quotes/market-price?instrumentId=1002&side=BUY" \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected price:

```json
{
  "instrumentId": 1002,
  "side": "BUY",
  "priceKopecks": 100001,
  "price": 100001
}
```

`priceKopecks` is one kopeck above `last` because quotes-service derives `ask = last + 1`.

## 7. Reset Account

```bash
curl -s -X POST http://127.0.0.1:8083/accounts/reset \
  -H "Authorization: Bearer $ACCESS" \
  -H "Content-Type: application/json" \
  -d '{"amount":"100000.00"}' | jq
```

Expected:

- `balance` is `100000.00`
- positions are cleared
- a `reset` operation is written

## 8. Deposit

```bash
curl -s -X POST http://127.0.0.1:8083/accounts/deposit \
  -H "Authorization: Bearer $ACCESS" \
  -H "Content-Type: application/json" \
  -d '{"amount":"5000.00"}' | jq
```

Expected:

- `balance` is increased
- a `deposit` operation is written

## 9. Buy Instrument

```bash
curl -s -X POST http://127.0.0.1:8083/orders \
  -H "Authorization: Bearer $ACCESS" \
  -H "Content-Type: application/json" \
  -d '{"instrumentId":1002,"ticker":"SHARE1002","side":"buy","type":"market","qty":2}' | jq
```

Expected:

- `status` is `filled`
- `side` is `buy`
- `quantity` is `2`
- an order row is created
- a `buy` operation is written
- cash balance decreases
- position `1002` is created or averaged

If status is `rejected` with `PRICE_UNAVAILABLE`, publish the test quote again and retry.

## 10. Check Portfolio

Summary:

```bash
curl -s http://127.0.0.1:8083/portfolio/summary \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected:

- `balance` is less than after deposit
- `portfolioValue` is greater than `0`
- `positions` contains `instrumentId=1002`
- `totalEquity = balance + portfolioValue`

Positions:

```bash
curl -s http://127.0.0.1:8083/portfolio/positions \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected:

- one position for `1002`
- `quantity` is `2`

## 11. Check Orders And Operations

Orders:

```bash
curl -s http://127.0.0.1:8083/orders \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected:

- one filled buy order

Operations:

```bash
curl -s http://127.0.0.1:8083/operations \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected operation types:

- `reset`
- `deposit`
- `buy`

Filter operations:

```bash
curl -s "http://127.0.0.1:8083/operations?type=buy&limit=10&offset=0" \
  -H "Authorization: Bearer $ACCESS" | jq
```

## 12. Check Analytics

Portfolio value:

```bash
curl -s "http://127.0.0.1:8083/analytics/portfolio/value?period=1d" \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected:

- `period` is `1d`
- `points` has one current snapshot

Allocation:

```bash
curl -s "http://127.0.0.1:8083/analytics/portfolio/allocation?period=1d" \
  -H "Authorization: Bearer $ACCESS" | jq
```

Expected:

- one allocation item for `1002`
- `percent` is near `100` when only one instrument is held

## 13. Useful Failure Checks

Insufficient funds:

```bash
curl -s -X POST http://127.0.0.1:8083/orders \
  -H "Authorization: Bearer $ACCESS" \
  -H "Content-Type: application/json" \
  -d '{"instrumentId":1002,"ticker":"SHARE1002","side":"buy","type":"market","qty":999999}' | jq
```

Expected:

- `status` is `rejected`
- `rejectReason` is `INSUFFICIENT_FUNDS`

Insufficient position:

```bash
curl -s -X POST http://127.0.0.1:8083/orders \
  -H "Authorization: Bearer $ACCESS" \
  -H "Content-Type: application/json" \
  -d '{"instrumentId":1002,"ticker":"SHARE1002","side":"sell","type":"market","qty":999999}' | jq
```

Expected:

- `status` is `rejected`
- `rejectReason` is `INSUFFICIENT_POSITION`

## 14. Local Database Caveat

`positions.instrument_id` is now `Long`, matching quotes `instrumentId`.

If your local `portfolio-db` was created before this change and has `instrument_id UUID`, the service can fail on position operations. For a local MVP database, recreate the portfolio Postgres volume or migrate the column.
