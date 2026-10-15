package dynamodb.domain.document_interface;

import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.PK2;
import static dynamodb.domain.DatabaseConstants.PK3;
import static dynamodb.domain.DatabaseConstants.SK0;
import static dynamodb.domain.DatabaseConstants.SK1;
import static dynamodb.domain.DatabaseConstants.SK2;
import static dynamodb.domain.DatabaseConstants.SK3;
import static java.util.UUID.randomUUID;
import dynamodb.domain.model.Entity;
import dynamodb.domain.model.FileEntry;
import dynamodb.domain.model.LogEntry;
import dynamodb.domain.model.Message;
import dynamodb.domain.model.Resource;
import dynamodb.domain.model.Ticket;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument.Builder;

public class EnhancedDocumentFactory {

    protected static final String RESOURCE = "Resource";
    protected static final String TICKET = "Ticket";
    protected static final String MESSAGE = "Message";
    protected static final String LOG_ENTRY = "LogEntry";
    protected static final String FILE = "File";
    protected static final String BY_TYPE_PATTERN = "%s:%s";
    protected static final String BY_CUSTOMER_PATTERN = "%s:%s:%s";
    private static final EnhancedType<? super String> STRING = EnhancedType.of(String.class);

    public static EnhancedDocument toEnhancedDocument(Entity entity) {
        var documentBuilder = EnhancedDocument.builder()
                                  .json(toString(entity))
                                  .put(PK0, getPK0(entity), STRING)
                                  .put(SK0, getSK0(entity), STRING);

        switch (entity) {
            case Resource resource -> putKeysForResource(resource, documentBuilder);
            case Ticket ticket -> putKeysForTicket(ticket, documentBuilder);
            case Message message -> putKeysForMessage(message, documentBuilder);
            case LogEntry logEntry -> putKeysForLogEntry(logEntry, documentBuilder);
            case FileEntry fileEntry -> putKeysForFile(fileEntry, documentBuilder);
            default -> throw new IllegalStateException("Unexpected value: " + entity);
        }

        return documentBuilder.build();
    }


    public static EnhancedDocument toEnhancedDocumentWithVersion(Entity entity) {
        var documentBuilder = EnhancedDocument.builder()
                                  .put(PK0, getPK0(entity), STRING)
                                  .put(SK0, getSK0(entity), STRING)
                                  .put("version", randomUUID().toString(), EnhancedType.of(String.class));
        return documentBuilder.build();
    }

    private static void putKeysForFile(FileEntry fileEntry, Builder documentBuilder) {
        documentBuilder.put(PK1, BY_TYPE_PATTERN.formatted(RESOURCE, fileEntry.resourceIdentifier()), STRING);
        documentBuilder.put(SK1, BY_TYPE_PATTERN.formatted(FILE, fileEntry.identifier()), STRING);
    }

    private static void putKeysForLogEntry(LogEntry logEntry, Builder documentBuilder) {
        documentBuilder.put(SK0, BY_TYPE_PATTERN.formatted(LOG_ENTRY, logEntry.identifier()), STRING);
    }

    private static void putKeysForMessage(Message message, Builder documentBuilder) {
        documentBuilder.put(PK1, BY_TYPE_PATTERN.formatted(RESOURCE, message.resourceIdentifier()), STRING);
        documentBuilder.put(SK1, BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier()), STRING);
        documentBuilder.put(PK2, BY_TYPE_PATTERN.formatted(TICKET, message.ticketIdentifier()), STRING);
        documentBuilder.put(SK2, BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier()), STRING);
        documentBuilder.put(PK3, BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier()), STRING);
        documentBuilder.put(SK3, BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier()), STRING);
    }

    private static void putKeysForTicket(Ticket ticket, Builder documentBuilder) {
        documentBuilder.put(PK1, BY_TYPE_PATTERN.formatted(RESOURCE, ticket.resourceIdentifier()), STRING);
        documentBuilder.put(SK1, BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier()), STRING);
        documentBuilder.put(PK2, BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier()), STRING);
        documentBuilder.put(SK2, BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier()), STRING);
        documentBuilder.put(PK3, BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier()), STRING);
        documentBuilder.put(SK3, BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier()), STRING);
    }

    private static void putKeysForResource(Resource resource, Builder documentBuilder) {
        documentBuilder.put(PK1, BY_TYPE_PATTERN.formatted(RESOURCE, resource.identifier()), STRING);
        documentBuilder.put(SK1, BY_TYPE_PATTERN.formatted(RESOURCE, resource.identifier()), STRING);
        documentBuilder.put(PK3, BY_TYPE_PATTERN.formatted(RESOURCE, resource.identifier()), STRING);
        documentBuilder.put(SK3, BY_TYPE_PATTERN.formatted(MESSAGE, resource.identifier()), STRING);
    }

    private static String getPK0(Entity entity) {
        return switch (entity) {
            case Resource resource ->
                BY_CUSTOMER_PATTERN.formatted(RESOURCE, resource.customerIdentifier(), resource.username());
            case Ticket ticket -> BY_CUSTOMER_PATTERN.formatted(TICKET, ticket.customerIdentifier(), ticket.username());
            case Message message ->
                BY_CUSTOMER_PATTERN.formatted(MESSAGE, message.customerIdentifier(), message.username());
            case LogEntry logEntry -> BY_TYPE_PATTERN.formatted(RESOURCE, logEntry.resourceIdentifier());
            case FileEntry fileEntry -> BY_TYPE_PATTERN.formatted(FILE, fileEntry.identifier());
            default -> throw new IllegalStateException("Unexpected value: " + entity);
        };
    }

    private static String getSK0(Entity entity) {
        return switch (entity) {
            case Resource resource -> BY_TYPE_PATTERN.formatted(RESOURCE, resource.identifier());
            case Ticket ticket -> BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier());
            case Message message -> BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier());
            case LogEntry logEntry -> BY_TYPE_PATTERN.formatted(LOG_ENTRY, logEntry.identifier());
            case FileEntry fileEntry -> BY_TYPE_PATTERN.formatted(FILE, fileEntry.identifier());
            default -> throw new IllegalStateException("Unexpected value: " + entity);
        };
    }

    private static String toString(Entity entity) {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write entity to string", e);
        }
    }
}
