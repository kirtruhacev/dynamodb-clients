package dynamodb.domain.high_level_interface;

import static dynamodb.domain.DatabaseConstants.GSI1;
import static dynamodb.domain.DatabaseConstants.GSI2;
import static dynamodb.domain.DatabaseConstants.GSI3;
import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.PK2;
import static dynamodb.domain.DatabaseConstants.TABLE_NAME;
import static java.util.UUID.randomUUID;
import static software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import dynamodb.domain.Repository;
import dynamodb.domain.high_level_interface.model.Dao;
import dynamodb.domain.high_level_interface.model.FileEntryDao;
import dynamodb.domain.high_level_interface.model.LogEntryDao;
import dynamodb.domain.high_level_interface.model.MessageDao;
import dynamodb.domain.high_level_interface.model.ResourceDao;
import dynamodb.domain.high_level_interface.model.TicketDao;
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
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

public class DynamoDbHighLevelClient implements Repository {

    private static final String UNEXPECTED_ENTITY = "Unexpected value: ";
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<ResourceDao> resourceTable;
    private final DynamoDbTable<TicketDao> ticketTable;
    private final DynamoDbTable<MessageDao> messageTable;
    private final DynamoDbTable<LogEntryDao> logEntryTable;
    private final DynamoDbTable<FileEntryDao> fileEntryTable;
    private final DynamoDbClient client;

    public DynamoDbHighLevelClient(DynamoDbClient client) {
        this.client = client;
        this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        this.resourceTable = enhancedClient.table(TABLE_NAME, fromBean(ResourceDao.class));
        this.ticketTable = enhancedClient.table(TABLE_NAME, fromBean(TicketDao.class));
        this.messageTable = enhancedClient.table(TABLE_NAME, fromBean(MessageDao.class));
        this.logEntryTable = enhancedClient.table(TABLE_NAME, fromBean(LogEntryDao.class));
        this.fileEntryTable = enhancedClient.table(TABLE_NAME, fromBean(FileEntryDao.class));
    }

    @Override
    public void save(Entity entity) {
        switch (entity) {
            case Resource resource -> resourceTable.putItem(ResourceDao.fromResource(resource));
            case Ticket ticket -> ticketTable.putItem(TicketDao.fromTicket(ticket));
            case Message message -> messageTable.putItem(MessageDao.fromMessage(message));
            case LogEntry logEntry -> logEntryTable.putItem(LogEntryDao.fromLogEntry(logEntry));
            case FileEntry fileEntry -> fileEntryTable.putItem(FileEntryDao.fromFileEntry(fileEntry));
            default -> throw new IllegalStateException(UNEXPECTED_ENTITY + entity);
        }
    }



    @Override
    public void updateVersion(Entity entity) {
        switch (entity) {
            case Resource resource -> stampVersion(resourceTable, withoutData(ResourceDao.fromResource(resource)));
            case Ticket ticket -> stampVersion(ticketTable, TicketDao.fromTicket(ticket));
            case Message message -> stampVersion(messageTable, MessageDao.fromMessage(message));
            case LogEntry logEntry -> stampVersion(logEntryTable, LogEntryDao.fromLogEntry(logEntry));
            case FileEntry fileEntry -> stampVersion(fileEntryTable, FileEntryDao.fromFileEntry(fileEntry));
            default -> throw new IllegalStateException(UNEXPECTED_ENTITY + entity);
        }
    }

    private static ResourceDao withoutData(ResourceDao resourceDao) {
        resourceDao.setData(null);
        return resourceDao;
    }

    private static <T extends Dao> void stampVersion(DynamoDbTable<T> table, T dao) {
        dao.setVersion(randomUUID().toString());
        table.updateItem(requestBuilder -> requestBuilder.item(dao).ignoreNulls(true));
    }

    @Override
    public void save(Entity... entities) {
        var requestBuilder = TransactWriteItemsEnhancedRequest.builder();
        Arrays.stream(entities).forEach(entity -> addPutItem(requestBuilder, entity));
        enhancedClient.transactWriteItems(requestBuilder.build());
    }

    private void addPutItem(TransactWriteItemsEnhancedRequest.Builder requestBuilder, Entity entity) {
        switch (entity) {
            case Resource resource -> requestBuilder.addPutItem(resourceTable, ResourceDao.fromResource(resource));
            case Ticket ticket -> requestBuilder.addPutItem(ticketTable, TicketDao.fromTicket(ticket));
            case Message message -> requestBuilder.addPutItem(messageTable, MessageDao.fromMessage(message));
            case LogEntry logEntry -> requestBuilder.addPutItem(logEntryTable, LogEntryDao.fromLogEntry(logEntry));
            case FileEntry fileEntry ->
                requestBuilder.addPutItem(fileEntryTable, FileEntryDao.fromFileEntry(fileEntry));
            default -> throw new IllegalStateException(UNEXPECTED_ENTITY + entity);
        }
    }


    @Override
    public Optional<Resource> getResourceByIdentifier(UUID identifier) {
        var partitionValue = ResourceDao.gsi3PartitionValue(identifier);
        return resourceTable.index(GSI3)
                   .query(queryWithPartitionValue(partitionValue))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(ResourceDao::toResource)
                   .findFirst();
    }

    @Override
    public Optional<Ticket> getTicketByIdentifier(UUID identifier) {
        var partitionValue = TicketDao.gsi3PartitionValue(identifier);
        return ticketTable.index(GSI3)
                   .query(queryWithPartitionValue(partitionValue))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(TicketDao::toTicket)
                   .findFirst();
    }

    @Override
    public Optional<Message> getMessageByIdentifier(UUID identifier) {
        var partitionValue = MessageDao.gsi3PartitionValue(identifier);
        return messageTable.index(GSI3)
                   .query(queryWithPartitionValue(partitionValue))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(MessageDao::toMessage)
                   .findFirst();
    }

    @Override
    public Optional<Ticket> getTicketWithMessagesByTicketIdentifier(UUID identifier) {
        var daoList = client.query(getQueryRequest(TicketDao.gsi3PartitionValue(identifier), GSI2, PK2))
                          .items()
                          .stream()
                          .map(this::toDao)
                          .toList();

        var ticket = daoList.stream().filter(TicketDao.class::isInstance).map(TicketDao.class::cast).findFirst();

        var messages = daoList.stream()
                           .filter(MessageDao.class::isInstance)
                           .map(MessageDao.class::cast)
                           .map(MessageDao::toMessage)
                           .toList();

        return ticket.map(ticketDao -> ticketDao.toTicket(messages));
    }

    @Override
    public List<Ticket> listTicketsByResourceIdentifier(UUID identifier) {
        return ticketTable.index(GSI1)
                   .query(queryWithPartitionValue(TicketDao.gsi1PartitionValue(identifier)))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(TicketDao::toTicket)
                   .toList();
    }

    @Override
    public List<Ticket> listTicketsWithMessagesByResourceIdentifier(UUID identifier) {
        var daoList = client.query(getQueryRequest(ResourceDao.gsi3PartitionValue(identifier), GSI1, PK1))
                          .items()
                          .stream()
                          .map(this::toDao)
                          .toList();

        var tickets = daoList.stream().filter(TicketDao.class::isInstance).map(TicketDao.class::cast).toList();

        var messages = daoList.stream().filter(MessageDao.class::isInstance).map(MessageDao.class::cast).toList();

        return tickets.stream()
                   .map(ticketDao -> toTicketWithMessages(ticketDao, messages))
                   .collect(Collectors.toList());
    }

    @Override
    public List<LogEntry> listLogEntriesByResourceIdentifier(UUID identifier) {
        return client.query(getQueryRequest(ResourceDao.gsi3PartitionValue(identifier), PK0))
                   .items()
                   .stream()
                   .map(this::toDao)
                   .filter(LogEntryDao.class::isInstance)
                   .map(LogEntryDao.class::cast)
                   .map(LogEntryDao::toLogEntry)
                   .toList();
    }

    @Override
    public List<FileEntry> listFilesByResourceIdentifier(UUID identifier) {
        return client.query(getQueryRequest(ResourceDao.gsi3PartitionValue(identifier), GSI1, PK1))
                   .items()
                   .stream()
                   .map(this::toDao)
                   .filter(FileEntryDao.class::isInstance)
                   .map(FileEntryDao.class::cast)
                   .map(FileEntryDao::toFileEntry)
                   .toList();
    }

    private static Ticket toTicketWithMessages(TicketDao ticketDao, List<MessageDao> messages) {
        var ticketMessages = messages.stream()
                                 .filter(
                                     messageDao -> messageDao.getTicketIdentifier().equals(ticketDao.getIdentifier()))
                                 .map(MessageDao::toMessage)
                                 .collect(Collectors.toList());
        return ticketDao.toTicket(ticketMessages);
    }

    private static QueryRequest getQueryRequest(String partitionValue, String indexName, String partitionKey) {
        return QueryRequest.builder()
                   .tableName(TABLE_NAME)
                   .indexName(indexName)
                   .keyConditions(Map.of(partitionKey, equalCondition(partitionValue)))
                   .build();
    }

    private static QueryRequest getQueryRequest(String partitionValue, String partitionKey) {
        return QueryRequest.builder()
                   .tableName(TABLE_NAME)
                   .keyConditions(Map.of(partitionKey, equalCondition(partitionValue)))
                   .build();
    }

    private static Condition equalCondition(String partitionValue) {
        return Condition.builder()
                   .comparisonOperator(ComparisonOperator.EQ)
                   .attributeValueList(AttributeValue.builder().s(partitionValue).build())
                   .build();
    }

    private static QueryConditional queryWithPartitionValue(String partitionValue) {
        return QueryConditional.keyEqualTo(keyWithPartitionValue(partitionValue));
    }

    private static Key keyWithPartitionValue(String partitionValue) {
        return Key.builder().partitionValue(partitionValue).build();
    }

    private Dao toDao(Map<String, AttributeValue> attributeValueMap) {
        try {
            var map = attributeValueMap.entrySet()
                          .stream()
                          .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().s()));
            var string = JsonUtils.dtoObjectMapper.writeValueAsString(map);
            return JsonUtils.dtoObjectMapper.readValue(string, Dao.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
