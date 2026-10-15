# dynamodb-clients

A side-by-side comparison of the three ways to talk to DynamoDB from the AWS SDK v2 for Java.

The same single-table schema, the same access patterns and the same `Repository` interface are implemented
three times — once per client style — so the trade-offs show up as real code rather than as documentation
claims. Tests run against DynamoDB Local (embedded, in-process), so the whole suite runs offline with
`./gradlew test`.

## The three clients

All three implement [`Repository`](commons/src/main/java/dynamodb/domain/Repository.java) and are swappable.

| Client | Package | What it works with | Key handling |
|---|---|---|---|
| **Low level** | `dynamodb.domain.low_level_interface` | `Map<String, AttributeValue>` | Written by hand, per request |
| **Enhanced document** | `dynamodb.domain.document_interface` | `EnhancedDocument` (JSON in, JSON out) | One factory builds keys for every entity type |
| **Enhanced high level** | `dynamodb.domain.high_level_interface` | `@DynamoDbBean` DAOs | Declared with annotations on each DAO |

### Low level — `DynamoDbClient`

Everything is explicit: attribute maps, key construction, serialisation, and the key condition of every query.

```java
var request = QueryRequest.builder()
                  .tableName(TABLE_NAME)
                  .indexName(indexName)
                  .keyConditionExpression(PARTITION_KEY_EQUALS + SORT_KEY_BEGINS_WITH)
                  .expressionAttributeNames(Map.of(PARTITION_KEY_NAME, partitionKey, SORT_KEY_NAME, sortKey))
                  .expressionAttributeValues(Map.of(PARTITION_VALUE, stringAttribute(partitionValue),
                                                    SORT_VALUE_PREFIX, stringAttribute(sortValuePrefix)))
                  .build();
```

The key layout the other two clients get from a table schema is hand-written here, in
[`ItemFactory`](repository/src/main/java/dynamodb/domain/low_level_interface/ItemFactory.java) — one `switch`
for the partition key, one for the sort key, one per entity type for the index keys. Nothing is hidden, and
nothing is done for you.

### Enhanced document — `DynamoDbTable<EnhancedDocument>`

```java
table.putItem(PutItemEnhancedRequest.builder(EnhancedDocument.class)
                  .item(toEnhancedDocument(entity))
                  .build());
```

The table schema is declared once, listing the key attributes of every index:

```java
TableSchema.documentSchemaBuilder()
    .addIndexPartitionKey(primaryIndexName(), PK0, S)
    .addIndexSortKey(primaryIndexName(), SK0, S)
    .addIndexPartitionKey(GSI1, PK1, S)
    ...
```

A single table handle serves every entity type, because an `EnhancedDocument` is just JSON plus keys. Records
are serialised with Jackson and the keys are layered on top in
[`EnhancedDocumentFactory`](repository/src/main/java/dynamodb/domain/document_interface/EnhancedDocumentFactory.java),
which is the only place that knows the key layout. Query results come back as documents and are read back into
the sealed `Entity` hierarchy by type tag — so a single query can return a ticket and its messages together and
still deserialise each item into the right record.

Transactional writes are a builder away:

```java
var requestbuilder = TransactWriteItemsEnhancedRequest.builder();
for (Entity entity : entities) {
    requestbuilder.addPutItem(table, toEnhancedDocument(entity));
}
client.transactWriteItems(requestbuilder.build());
```

### Enhanced high level — `DynamoDbTable<SomeDao>`

```java
this.resourceTable = enhancedClient.table(TABLE_NAME, fromBean(ResourceDao.class));
this.ticketTable = enhancedClient.table(TABLE_NAME, fromBean(TicketDao.class));
...
```

Keys are declared as annotations on the DAO, which reads well:

```java
@DynamoDbSecondaryPartitionKey(indexNames = {GSI3})
@DynamoDbAttribute(PK3)
@JsonProperty(PK3)
public String getGsi3PartitionKey() {
    return "%s:%s".formatted(TYPE, getIdentifier());
}
```

The cost is visible in the constructor: beans are mapped per class, so one table handle is needed per entity
type, and the DAOs are mutable JavaBeans — `@DynamoDbBean` requires a no-arg constructor and setters, which
rules out records. Worse, a heterogeneous query (a ticket plus its messages, which live under the same
partition key) does not fit the bean mapper at all; `DynamoDbHighLevelClient` drops back to the **low level**
client for those and deserialises the raw attribute map through a Jackson type hierarchy:

```java
var daoList = client.query(getQueryRequest(TicketDao.gsi3PartitionValue(identifier), GSI2, PK2))
                  .items()
                  .stream()
                  .map(this::toDao)
                  .toList();
```

## What the comparison shows

- **The bean mapper breaks on reads, not writes.** `addPutItem(table, item)` is generic per call, so one
  transaction carries a `ResourceDao`, a `TicketDao` and a `MessageDao` together — at write time the caller
  holds a concrete type and can name its table. A query returns items whose types are unknown until you
  inspect them, so `getTicketWithMessagesByTicketIdentifier` drops to a raw `client.query`.
- **The document client fits single-table design.** One table handle, one place where keys are built, item
  types told apart after the query by their own type tag — and entities stay `record`s rather than mutable DAOs.
- **The trade is when failure happens.** Beans buy compile-time key safety and pay in flexibility; documents
  and raw attribute maps buy flexibility and fail only at runtime. The low level client hand-writes every key
  condition, so a typo surfaces as a query quietly returning nothing; a bean partial update expresses "leave
  this field alone" as a null plus `ignoreNulls(true)`, where dropping the flag rewrites the whole entity.
- **Scope.** A verdict on single-table design, not on the bean mapper: with one entity type per table the trade
  flips and compile-time safety costs nothing you need. 28 tests on embedded DynamoDB Local also say nothing
  about pagination past one page, throttling, retries or performance.

## Table design

A single table with four global secondary indexes, all keys `String`, created by
[`DynamoDBLocal`](test-utils/src/main/java/dynamodb/domain/DynamoDBLocal.java).

| Index Name | Partition Key | Sort Key | Used For |
|------------|---------------|----------|----------|
| Primary    | PK0 | SK0 | Get entity by identifier |
| GSI1       | PK1 | SK1 | Resource-related queries (e.g. tickets, files by resource) |
| GSI2       | PK2 | SK2 | Ticket with messages by ticket identifier |
| GSI3       | PK3 | SK3 | Entity by type and identifier (e.g. get by ID) |

Key values are `Type:{uuid}` (`Resource:3f2a…`), or `Type:{customer}:{username}` for the primary partition key.

The table also carries a spare GSI4 that no access pattern uses yet.

### Access patterns

| Access Pattern | Index | Partition Key Value | Sort Key Value | Notes |
|---|---|---|---|---|
| Get Resource by Identifier | GSI3 | `Resource:{uuid}` | - | Single resource fetch |
| Get Ticket by Identifier | GSI3 | `Ticket:{uuid}` | - | Single ticket fetch |
| Get Message by Identifier | GSI3 | `Message:{uuid}` | - | Single message fetch |
| Get Ticket with Messages by Ticket Identifier | GSI2 | `Ticket:{uuid}` | - | Returns ticket and its messages |
| List Tickets by Resource Identifier | GSI1 | `Resource:{uuid}` | `Ticket` (begins with) | All tickets for a resource |
| List Tickets with Messages by Resource Identifier | GSI1 | `Resource:{uuid}` | - | Tickets and their messages for a resource |
| List Log Entries by Resource Identifier | Primary | `Resource:{uuid}` | - | All log entries for a resource |
| List Files by Resource Identifier | GSI1 | `Resource:{uuid}` | `File` (begins with) | All files for a resource |

### Entities

`Entity` is a sealed interface over `Resource`, `Ticket`, `Message`, `LogEntry` and `FileEntry` — a publishing
domain, where a resource collects tickets (each with messages), log entries and files. All are records.

## Running

Requires JDK 21 — the clients use pattern matching for `switch` over the sealed `Entity` hierarchy. Gradle
comes from the wrapper.

```sh
./gradlew test
```

DynamoDB Local runs embedded in the test JVM, so no Docker and no AWS credentials are needed. Each test
creates the table in `@BeforeEach` and drops it in `@AfterEach`.

