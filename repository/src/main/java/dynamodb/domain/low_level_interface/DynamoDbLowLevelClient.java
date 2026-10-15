package dynamodb.domain.low_level_interface;

import static dynamodb.domain.DatabaseConstants.GSI1;
import static dynamodb.domain.DatabaseConstants.GSI2;
import static dynamodb.domain.DatabaseConstants.GSI3;
import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.PK2;
import static dynamodb.domain.DatabaseConstants.PK3;
import static dynamodb.domain.DatabaseConstants.SK1;
import static dynamodb.domain.DatabaseConstants.TABLE_NAME;
import static dynamodb.domain.low_level_interface.ItemFactory.BY_TYPE_PATTERN;
import static dynamodb.domain.low_level_interface.ItemFactory.FILE;
import static dynamodb.domain.low_level_interface.ItemFactory.MESSAGE;
import static dynamodb.domain.low_level_interface.ItemFactory.RESOURCE;
import static dynamodb.domain.low_level_interface.ItemFactory.TICKET;
import static dynamodb.domain.low_level_interface.ItemFactory.stringAttribute;
import static dynamodb.domain.low_level_interface.ItemFactory.toItem;
import static dynamodb.domain.low_level_interface.ItemFactory.toKey;
import static java.util.UUID.randomUUID;
import dynamodb.domain.Repository;
import dynamodb.domain.model.Entity;
import dynamodb.domain.model.FileEntry;
import dynamodb.domain.model.LogEntry;
import dynamodb.domain.model.Message;
import dynamodb.domain.model.Resource;
import dynamodb.domain.model.Ticket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DynamoDbLowLevelClient implements Repository {

    private static final String VERSION_ATTRIBUTE = "version";
    private static final String VERSION_NAME = "#version";
    private static final String VERSION_VALUE = ":version";
    private static final String SET_VERSION = "SET #version = :version";
    private static final String PARTITION_KEY_NAME = "#partitionKey";
    private static final String SORT_KEY_NAME = "#sortKey";
    private static final String PARTITION_VALUE = ":partitionValue";
    private static final String SORT_VALUE_PREFIX = ":sortValuePrefix";
    private static final String PARTITION_KEY_EQUALS = "#partitionKey = :partitionValue";
    private static final String SORT_KEY_BEGINS_WITH = " and begins_with(#sortKey, :sortValuePrefix)";

    private final DynamoDbClient client;

    public DynamoDbLowLevelClient(DynamoDbClient client) {
        this.client = client;
    }

    @Override
    public void save(Entity entity) {
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(toItem(entity)).build());
    }

    @Override
    public void save(Entity... entities) {
        var transactItems = Arrays.stream(entities).map(DynamoDbLowLevelClient::toTransactWriteItem).toList();
        client.transactWriteItems(TransactWriteItemsRequest.builder().transactItems(transactItems).build());
    }




    @Override
    public void updateVersion(Entity entity) {
        var request = UpdateItemRequest.builder()
                          .tableName(TABLE_NAME)
                          .key(toKey(entity))
                          .updateExpression(SET_VERSION)
                          .expressionAttributeNames(Map.of(VERSION_NAME, VERSION_ATTRIBUTE))
                          .expressionAttributeValues(Map.of(VERSION_VALUE, stringAttribute(randomUUID().toString())))
                          .build();
        client.updateItem(request);
    }

    @Override
    public Optional<Resource> getResourceByIdentifier(UUID identifier) {
        return getEntityByIdentifier(RESOURCE, identifier, Resource.class);
    }

    @Override
    public Optional<Ticket> getTicketByIdentifier(UUID identifier) {
        return getEntityByIdentifier(TICKET, identifier, Ticket.class);
    }

    @Override
    public Optional<Message> getMessageByIdentifier(UUID identifier) {
        return getEntityByIdentifier(MESSAGE, identifier, Message.class);
    }

    @Override
    public Optional<Ticket> getTicketWithMessagesByTicketIdentifier(UUID identifier) {
        var entities = queryIndex(GSI2, PK2, BY_TYPE_PATTERN.formatted(TICKET, identifier));
        var messages = entitiesOfType(entities, Message.class);
        return entitiesOfType(entities, Ticket.class)
                   .stream()
                   .findFirst()
                   .map(ticket -> withMessages(ticket, messages));
    }

    @Override
    public List<Ticket> listTicketsByResourceIdentifier(UUID identifier) {
        var entities = queryIndexWithSortKeyPrefix(GSI1, PK1, byResource(identifier), SK1, TICKET);
        return entitiesOfType(entities, Ticket.class);
    }

    @Override
    public List<Ticket> listTicketsWithMessagesByResourceIdentifier(UUID identifier) {
        var entities = queryIndex(GSI1, PK1, byResource(identifier));
        var messages = entitiesOfType(entities, Message.class);
        return entitiesOfType(entities, Ticket.class)
                   .stream()
                   .map(ticket -> withMessages(ticket, messages))
                   .toList();
    }

    @Override
    public List<LogEntry> listLogEntriesByResourceIdentifier(UUID identifier) {
        return entitiesOfType(queryTable(PK0, byResource(identifier)), LogEntry.class);
    }

    @Override
    public List<FileEntry> listFilesByResourceIdentifier(UUID identifier) {
        var entities = queryIndexWithSortKeyPrefix(GSI1, PK1, byResource(identifier), SK1, FILE);
        return entitiesOfType(entities, FileEntry.class);
    }

    private static String byResource(UUID identifier) {
        return BY_TYPE_PATTERN.formatted(RESOURCE, identifier);
    }

    private static TransactWriteItem toTransactWriteItem(Entity entity) {
        return TransactWriteItem.builder()
                   .put(Put.builder().tableName(TABLE_NAME).item(toItem(entity)).build())
                   .build();
    }

    private static <T extends Entity> List<T> entitiesOfType(List<Entity> entities, Class<T> entityType) {
        return entities.stream().filter(entityType::isInstance).map(entityType::cast).toList();
    }

    private static Ticket withMessages(Ticket ticket, List<Message> messages) {
        var ticketMessages = messages.stream()
                                 .filter(message -> message.ticketIdentifier().equals(ticket.identifier()))
                                 .toList();
        return new Ticket(ticket.identifier(), ticket.resourceIdentifier(), ticket.customerIdentifier(),
                          ticket.username(), ticketMessages, ticket.modifiedDate());
    }

    private static QueryRequest.Builder queryBuilder(String partitionKey, String partitionValue) {
        return QueryRequest.builder()
                   .tableName(TABLE_NAME)
                   .keyConditionExpression(PARTITION_KEY_EQUALS)
                   .expressionAttributeNames(Map.of(PARTITION_KEY_NAME, partitionKey))
                   .expressionAttributeValues(Map.of(PARTITION_VALUE, stringAttribute(partitionValue)));
    }

    private <T extends Entity> Optional<T> getEntityByIdentifier(String type, UUID identifier, Class<T> entityType) {
        var entities = queryIndex(GSI3, PK3, BY_TYPE_PATTERN.formatted(type, identifier));
        return entitiesOfType(entities, entityType).stream().findFirst();
    }

    private List<Entity> queryTable(String partitionKey, String partitionValue) {
        return runQuery(queryBuilder(partitionKey, partitionValue).build());
    }

    private List<Entity> queryIndex(String indexName, String partitionKey, String partitionValue) {
        return runQuery(queryBuilder(partitionKey, partitionValue).indexName(indexName).build());
    }

    private List<Entity> queryIndexWithSortKeyPrefix(String indexName, String partitionKey, String partitionValue,
                                                     String sortKey, String sortValuePrefix) {
        var request = QueryRequest.builder()
                          .tableName(TABLE_NAME)
                          .indexName(indexName)
                          .keyConditionExpression(PARTITION_KEY_EQUALS + SORT_KEY_BEGINS_WITH)
                          .expressionAttributeNames(Map.of(PARTITION_KEY_NAME, partitionKey, SORT_KEY_NAME, sortKey))
                          .expressionAttributeValues(Map.of(PARTITION_VALUE, stringAttribute(partitionValue),
                                                            SORT_VALUE_PREFIX, stringAttribute(sortValuePrefix)))
                          .build();
        return runQuery(request);
    }

    private List<Entity> runQuery(QueryRequest request) {
        return client.queryPaginator(request).items().stream().map(ItemFactory::toEntity).toList();
    }
}
