# Expense Splitter

A Splitwise-style REST API built with **Spring Boot 3** and **Java 17**. Create users and groups, record shared expenses, see who owes what, and get the shortest list of payments that settles everyone up.

> Example: Alice paid 3000 for dinner and Bob paid 900 for a cab, both shared by Alice, Bob and Charlie. Instead of everyone paying everyone, the app tells you: **Charlie pays Alice 1300** and **Bob pays Alice 400**. Done.

## Features

- **Users** - create and list users (unique email)
- **Groups** - create a group, add and remove members
- **Expenses** - add an expense to a group: who paid, how much, and who shares it (split equally, to the cent)
- **Balances** - net balance per user for a group
- **Settle-up** - the minimum set of payments to clear all debts (greedy algorithm with two heaps)
- **Validation and error handling** - Bean Validation on every request body, one global exception handler, correct HTTP status codes (400 / 404 / 409 / 500)

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 17 (plain Java, no Lombok) |
| Framework | Spring Boot 3.5 (Spring Web, Spring Data JPA, Bean Validation) |
| Database | H2 in-memory by default; MySQL via an optional profile |
| Build | Maven |
| Tests | JUnit 5, Spring Boot Test / MockMvc |

## Project structure

```
expense-splitter
├── pom.xml
├── postman/expense-splitter.postman_collection.json
└── src
    ├── main
    │   ├── java/com/example/expensesplitter
    │   │   ├── ExpenseSplitterApplication.java   # entry point
    │   │   ├── controller/   # HTTP layer: URLs, status codes, @Valid
    │   │   ├── service/      # business rules + the two algorithms
    │   │   │   ├── SettlementCalculator.java     # settle-up (two heaps)
    │   │   │   └── SplitCalculator.java          # equal split to the cent
    │   │   ├── repository/   # Spring Data JPA interfaces
    │   │   ├── entity/       # JPA entities: User, ExpenseGroup, Expense
    │   │   ├── dto/          # request / response objects (Java records)
    │   │   └── exception/    # custom exceptions + GlobalExceptionHandler
    │   └── resources
    │       ├── application.properties            # H2 (default)
    │       └── application-mysql.properties      # optional MySQL profile
    └── test/java/...         # algorithm unit tests + API integration test
```

**How a request flows:** `Controller` (HTTP in/out) -> `Service` (rules and calculations) -> `Repository` (database). Controllers never touch the database and never build error responses; services just throw exceptions, and `GlobalExceptionHandler` turns them into JSON.

## How to run

**Prerequisites:** JDK 17 or newer and Maven 3.6+.

```bash
mvn spring-boot:run
```

The API is now live at `http://localhost:8080`. No database setup is needed: H2 runs in memory (data resets when you stop the app).

Other useful commands:

```bash
mvn test                # run all tests
mvn clean package       # build target/expense-splitter-1.0.0.jar
java -jar target/expense-splitter-1.0.0.jar
```

**H2 console** (to look at the tables): open `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:expensedb`, user `sa`, empty password.

**Use MySQL instead:** create a database (`CREATE DATABASE expense_splitter;`), edit the username/password in `src/main/resources/application-mysql.properties` (or set `DB_USERNAME` / `DB_PASSWORD`), then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

**Postman:** import `postman/expense-splitter.postman_collection.json`, then run the collection in order. It saves the created ids into variables automatically and includes error-case requests.

## API endpoints

| Method | URL | Description | Success |
|--------|-----|-------------|---------|
| POST | `/users` | Create a user | 201 |
| GET | `/users` | List users | 200 |
| GET | `/users/{id}` | Get one user | 200 |
| POST | `/groups` | Create a group (optionally with members) | 201 |
| GET | `/groups` | List groups | 200 |
| GET | `/groups/{id}` | Get a group with its members | 200 |
| POST | `/groups/{id}/members` | Add a member | 200 |
| DELETE | `/groups/{id}/members/{userId}` | Remove a member | 200 |
| POST | `/groups/{id}/expenses` | Add an expense | 201 |
| GET | `/groups/{id}/expenses` | List a group's expenses | 200 |
| GET | `/groups/{id}/balances` | Net balance per user | 200 |
| GET | `/groups/{id}/settlements` | Payments that settle all debts | 200 |

### Create a user

`POST /users`

```json
{ "name": "Alice", "email": "alice@example.com" }
```

Response `201 Created`:

```json
{ "id": 1, "name": "Alice", "email": "alice@example.com" }
```

Create Bob (id 2) and Charlie (id 3) the same way.

### Create a group

`POST /groups` (`memberIds` is optional)

```json
{ "name": "Goa Trip", "memberIds": [1, 2, 3] }
```

Response `201 Created`:

```json
{
  "id": 1,
  "name": "Goa Trip",
  "members": [
    { "id": 1, "name": "Alice",   "email": "alice@example.com" },
    { "id": 2, "name": "Bob",     "email": "bob@example.com" },
    { "id": 3, "name": "Charlie", "email": "charlie@example.com" }
  ]
}
```

### Add / remove a member

`POST /groups/1/members`

```json
{ "userId": 4 }
```

Returns the updated group (`200 OK`). `DELETE /groups/1/members/4` removes a member and returns the updated group.

Rules: adding someone who is already in the group returns `409`; removing someone who paid for or shares any expense returns `409` (it would silently change everyone else's balances).

### Add an expense

`POST /groups/1/expenses` - Alice paid 3000 for dinner, shared equally by all three:

```json
{
  "description": "Dinner",
  "amount": 3000.00,
  "payerId": 1,
  "participantIds": [1, 2, 3]
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "groupId": 1,
  "description": "Dinner",
  "amount": 3000.00,
  "payer": { "id": 1, "name": "Alice", "email": "alice@example.com" },
  "participants": [
    { "id": 1, "name": "Alice",   "email": "alice@example.com" },
    { "id": 2, "name": "Bob",     "email": "bob@example.com" },
    { "id": 3, "name": "Charlie", "email": "charlie@example.com" }
  ],
  "createdAt": "2026-10-13T10:15:30.123456"
}
```

Then add a second expense: Bob paid 900 for a cab, shared by all three (`"amount": 900, "payerId": 2, "participantIds": [1, 2, 3]`).

The payer and every participant must be members of the group (otherwise `400`). Amounts must be at least 0.01 with at most 2 decimal places.

### Get balances

`GET /groups/1/balances`

```json
[
  { "userId": 1, "userName": "Alice",   "balance": 1700.00 },
  { "userId": 2, "userName": "Bob",     "balance": -400.00 },
  { "userId": 3, "userName": "Charlie", "balance": -1300.00 }
]
```

**Positive = is owed money. Negative = owes money.** Alice: paid 3000, her share of both expenses is 1300, so she is owed 1700. The balances always add up to zero.

### Get settlements (who pays whom)

`GET /groups/1/settlements`

```json
[
  { "fromUserId": 3, "fromUserName": "Charlie", "toUserId": 1, "toUserName": "Alice", "amount": 1300.00 },
  { "fromUserId": 2, "fromUserName": "Bob",     "toUserId": 1, "toUserName": "Alice", "amount": 400.00 }
]
```

If everyone is already square, the list is empty (`[]`).

### Error responses

Every error has the same JSON shape. Validation errors add a `fieldErrors` map.

`POST /users` with `{ "name": "", "email": "not-an-email" }` returns `400 Bad Request`:

```json
{
  "timestamp": "2026-10-13T10:20:00.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/users",
  "fieldErrors": {
    "name": "name is required",
    "email": "email must be a valid email address"
  }
}
```

| Status | When |
|--------|------|
| 400 Bad Request | Validation failed, malformed JSON, invalid id format, payer/participant not in the group |
| 404 Not Found | User or group does not exist, unknown URL |
| 405 / 415 | Wrong HTTP method / content type is not JSON |
| 409 Conflict | Duplicate email, member already in group, removing a member who has expenses |
| 500 Internal Server Error | Anything unexpected (details are logged, never sent to the client) |

## How the settlement algorithm works

**Step 1 - balances.** For every expense, the payer gets `+amount` and each participant gets `-share`. Adding this up over all expenses gives each person's net balance. Positive means they are owed money (a *creditor*); negative means they owe money (a *debtor*). All balances sum to zero.

**Step 2 - two heaps.** Put all creditors in one max-heap and all debtors in another (the largest amount is always on top). Then repeat:

1. Take the biggest creditor and the biggest debtor.
2. The debtor pays the creditor `min(credit, debt)`. This fully settles at least one of the two.
3. Whoever still has a remainder goes back onto their heap.
4. Stop when the heaps are empty.

**Worked example.** Balances: A +50, B +30, C -45, D -35.

| Round | Biggest creditor | Biggest debtor | Payment | Left over |
|-------|------------------|----------------|---------|-----------|
| 1 | A (50) | C (45) | C pays A 45 | A still owed 5 |
| 2 | A (5)  | D (35) | D pays A 5  | D still owes 30 |
| 3 | B (30) | D (30) | D pays B 30 | everyone is at 0 |

Three payments settle four people (instead of up to 4 x 3 / 2 = 6 payments if everyone paid everyone directly).

**Why it is efficient.** Every payment settles at least one person completely, so for *n* people with a non-zero balance there are **at most n - 1 payments**. Each heap operation costs O(log n), so the whole thing runs in **O(n log n)**.

**Honest caveat.** Finding the absolute minimum number of payments in every possible case is a harder problem (it relates to splitting the group into the largest number of sub-groups whose balances each sum to zero). The two-heap greedy method is the standard practical answer: it guarantees at most n - 1 payments and gives the true minimum in the vast majority of real groups.

**Rounding to the cent.** Money uses `BigDecimal`, never `double`. When an amount does not divide evenly (100.00 between 3 people), everyone gets 33.33 and the leftover cent goes to the first participant by id: 33.34 + 33.33 + 33.33 = 100.00. Shares therefore always add up to exactly the expense amount, and balances always sum to zero.

The code lives in `SettlementCalculator` and `SplitCalculator` (plain Java with no database access) and is covered by unit tests in `src/test/java/.../service`.

## Design decisions

- **Balances are computed, not stored.** They are derived from the expenses on every request, so they can never drift out of sync.
- **DTOs instead of entities in the API.** The JSON shape is independent of the database model.
- **Constructor injection** everywhere (no field `@Autowired`), which makes classes easy to test.
- **`users` and `expense_groups` table names** avoid SQL reserved words (`user`, `group`).

## Possible improvements

- Unequal splits (exact amounts or percentages)
- Edit / delete expenses
- Authentication (Spring Security + JWT)
- Pagination on list endpoints
- Flyway migrations instead of `ddl-auto=update`
- Swagger / OpenAPI documentation
