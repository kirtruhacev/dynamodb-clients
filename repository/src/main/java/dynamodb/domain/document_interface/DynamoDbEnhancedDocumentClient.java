package dynamodb.domain.document_interface;

import static dynamodb.domain.DatabaseConstants.GSI1;
import static dynamodb.domain.DatabaseConstants.GSI2;
import static dynamodb.domain.DatabaseConstants.GSI3;
import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.PK2;
import static dynamodb.domain.DatabaseConstants.PK3;
import static dynamodb.domain.DatabaseConstants.SK0;
import static dynamodb.domain.DatabaseConstants.SK1;
import static dynamodb.domain.DatabaseConstants.SK2;
import static dynamodb.domain.DatabaseConstants.SK3;
import static dynamodb.domain.DatabaseConstants.TABLE_NAME;
import static dynamodb.domain.document_interface.EnhancedDocumentFactory.FILE;
import static dynamodb.domain.document_interface.EnhancedDocumentFactory.toEnhancedDocument;
import static dynamodb.domain.document_interface.EnhancedDocumentFactory.toEnhancedDocumentWithVersion;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeValueType.S;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;
import dynamodb.domain.Repository;
import dynamodb.domain.model.Entity;
import dynamodb.domain.model.FileEntry;
import dynamodb.domain.model.LogEntry;
import dynamodb.domain.model.Message;
import dynamodb.domain.model.Resource;
import dynamodb.domain.model.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbEnhancedDocumentClient implements Repository {

    protected static final String RESOURCE = "Resource";
    protected static final String TICKET = "Ticket";
    protected static final String MESSAGE = "Message";
    protected static final String BY_TYPE_PATTERN = "%s:%s";
    private final DynamoDbTable<EnhancedDocument> table;
    private final DynamoDbEnhancedClient client;

    public DynamoDbEnhancedDocumentClient(DynamoDbClient client) {
        this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        this.table = this.client.table(TABLE_NAME, documentTableSchema());
    }

    @Override
    public void save(Entity entity) {
        table.putItem(PutItemEnhancedRequest.builder(EnhancedDocument.class).item(toEnhancedDocument(entity)).build());
    }


    @Override
    public void updateVersion(Entity entity) {
        table.updateItem(toEnhancedDocumentWithVersion(entity));
    }

    @Override
    public void save(Entity... entities) {
        var requestbuilder = TransactWriteItemsEnhancedRequest.builder();
        for (Entity entity : entities) {
            requestbuilder.addPutItem(table, toEnhancedDocument(entity));
        }
        client.transactWriteItems(requestbuilder.build());
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
        var queryConditional = getQueryConditional(BY_TYPE_PATTERN.formatted(TICKET, identifier));
        var entities = listEntities(GSI2, queryConditional);

        var ticketOptional = entities.stream().filter(Ticket.class::isInstance).map(Ticket.class::cast).findFirst();

        var messages = entities.stream().filter(Message.class::isInstance).map(Message.class::cast).toList();

        return ticketOptional.map(ticket -> toTicketWithMessages(ticket, messages));
    }

    @Override
    public List<Ticket> listTicketsByResourceIdentifier(UUID identifier) {
        var queryConditional = getQueryConditional(BY_TYPE_PATTERN.formatted(RESOURCE, identifier), TICKET);
        var entities = listEntities(GSI1, queryConditional);
        return entities.stream().filter(Ticket.class::isInstance).map(Ticket.class::cast).toList();
    }

    @Override
    public List<Ticket> listTicketsWithMessagesByResourceIdentifier(UUID identifier) {
        var queryConditional = getQueryConditional(BY_TYPE_PATTERN.formatted(RESOURCE, identifier));
        var entities = listEntities(GSI1, queryConditional);

        var tickets = entities.stream().filter(Ticket.class::isInstance).map(Ticket.class::cast).toList();

        var messages = entities.stream().filter(Message.class::isInstance).map(Message.class::cast).toList();

        return tickets.stream().map(ticket -> toTicketWithMessages(ticket, messages)).collect(Collectors.toList());
    }

    @Override
    public List<LogEntry> listLogEntriesByResourceIdentifier(UUID identifier) {
        return table.query(getQueryConditional(BY_TYPE_PATTERN.formatted(RESOURCE, identifier)))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(enhancedDocument -> convertToEntity(enhancedDocument, Entity.class))
                   .filter(LogEntry.class::isInstance)
                   .map(LogEntry.class::cast)
                   .toList();
    }

    @Override
    public List<FileEntry> listFilesByResourceIdentifier(UUID identifier) {
        var queryConditional = getQueryConditional(BY_TYPE_PATTERN.formatted(RESOURCE, identifier), FILE);
        var entities = listEntities(GSI1, queryConditional);
        return entities.stream().filter(FileEntry.class::isInstance).map(FileEntry.class::cast).toList();
    }

    private static DocumentTableSchema documentTableSchema() {
        return TableSchema.documentSchemaBuilder()
                   .addIndexPartitionKey(primaryIndexName(), PK0, S)
                   .addIndexSortKey(primaryIndexName(), SK0, S)
                   .addIndexPartitionKey(GSI1, PK1, S)
                   .addIndexSortKey(GSI1, SK1, S)
                   .addIndexPartitionKey(GSI2, PK2, S)
                   .addIndexSortKey(GSI2, SK2, S)
                   .addIndexPartitionKey(GSI3, PK3, S)
                   .addIndexSortKey(GSI3, SK3, S)
                   .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
                   .build();
    }

    private static Ticket toTicketWithMessages(Ticket ticket, List<Message> messages) {
        var ticketMessages = messages.stream()
                                 .filter(message -> message.ticketIdentifier().equals(ticket.identifier()))
                                 .toList();
        return new Ticket(ticket.identifier(), ticket.resourceIdentifier(), ticket.customerIdentifier(),
                          ticket.username(), ticketMessages, ticket.modifiedDate());
    }

    private static QueryConditional getQueryConditional(String partitionValue) {
        return QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionValue).build());
    }

    private static QueryConditional getQueryConditional(String partitionValue, String sortKeyBeginsWith) {
        return QueryEnhancedRequest.builder()
                   .queryConditional(getQueryConditionalSortKeyBeginsWith(partitionValue, sortKeyBeginsWith))
                   .build()
                   .queryConditional();
    }

    private static QueryConditional getQueryConditionalSortKeyBeginsWith(String partitionValue,
                                                                         String sortKeyBeginsWith) {
        return QueryConditional.sortBeginsWith(
            Key.builder().partitionValue(partitionValue).sortValue(sortKeyBeginsWith).build());
    }

    private List<Entity> listEntities(String index, QueryConditional queryConditional) {
        return table.index(index)
                   .query(queryConditional)
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(enhancedDocument -> convertToEntity(enhancedDocument, Entity.class))
                   .toList();
    }

    private <T extends Entity> Optional<T> getEntityByIdentifier(String type, UUID identifier, Class<T> targetClass) {
        return table.index(GSI3)
                   .query(getQueryConditional(BY_TYPE_PATTERN.formatted(type, identifier)))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .findFirst()
                   .map(enhancedDocument -> convertToEntity(enhancedDocument, targetClass))
                   .filter(targetClass::isInstance)
                   .map(targetClass::cast);
    }

    private Entity convertToEntity(EnhancedDocument enhancedDocument, Class<? extends Entity> valueType) {
        try {
            return JsonUtils.dtoObjectMapper.readValue(enhancedDocument.toJson(), valueType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert document to entity", e);
        }
    }
}
