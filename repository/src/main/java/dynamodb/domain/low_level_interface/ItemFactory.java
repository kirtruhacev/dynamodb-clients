package dynamodb.domain.low_level_interface;

import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.PK2;
import static dynamodb.domain.DatabaseConstants.PK3;
import static dynamodb.domain.DatabaseConstants.SK0;
import static dynamodb.domain.DatabaseConstants.SK1;
import static dynamodb.domain.DatabaseConstants.SK2;
import static dynamodb.domain.DatabaseConstants.SK3;
import dynamodb.domain.model.Entity;
import dynamodb.domain.model.FileEntry;
import dynamodb.domain.model.LogEntry;
import dynamodb.domain.model.Message;
import dynamodb.domain.model.Resource;
import dynamodb.domain.model.Ticket;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public final class ItemFactory {

    public static final String RESOURCE = "Resource";
    public static final String TICKET = "Ticket";
    public static final String MESSAGE = "Message";
    public static final String FILE = "File";
    public static final String BY_TYPE_PATTERN = "%s:%s";
    private static final String LOG_ENTRY = "LogEntry";
    private static final String BY_CUSTOMER_PATTERN = "%s:%s:%s";
    private static final String DATA = "data";
    private static final String UNSUPPORTED_ENTITY = "Unsupported entity: %s";
    private static final String SERIALIZATION_FAILED = "Failed to serialize entity";
    private static final String DESERIALIZATION_FAILED = "Failed to deserialize entity";

    private ItemFactory() {
    }

    public static Map<String, AttributeValue> toItem(Entity entity) {
        var item = new HashMap<String, AttributeValue>();
        item.put(PK0, stringAttribute(partitionKey(entity)));
        item.put(SK0, stringAttribute(sortKey(entity)));
        item.put(DATA, stringAttribute(toJson(entity)));
        item.putAll(secondaryIndexKeys(entity));
        return Map.copyOf(item);
    }

    public static Map<String, AttributeValue> toKey(Entity entity) {
        return Map.of(PK0, stringAttribute(partitionKey(entity)), SK0, stringAttribute(sortKey(entity)));
    }

    public static Entity toEntity(Map<String, AttributeValue> item) {
        try {
            return JsonUtils.dtoObjectMapper.readValue(item.get(DATA).s(), Entity.class);
        } catch (Exception exception) {
            throw new IllegalStateException(DESERIALIZATION_FAILED, exception);
        }
    }

    public static AttributeValue stringAttribute(String value) {
        return AttributeValue.fromS(value);
    }

    private static Map<String, AttributeValue> secondaryIndexKeys(Entity entity) {
        return switch (entity) {
            case Resource resource -> resourceKeys(resource);
            case Ticket ticket -> ticketKeys(ticket);
            case Message message -> messageKeys(message);
            case FileEntry fileEntry -> fileKeys(fileEntry);
            case LogEntry logEntry -> Map.of();
            default -> throw new IllegalStateException(UNSUPPORTED_ENTITY.formatted(entity));
        };
    }

    private static Map<String, AttributeValue> resourceKeys(Resource resource) {
        var byResource = stringAttribute(BY_TYPE_PATTERN.formatted(RESOURCE, resource.identifier()));
        return Map.of(PK1, byResource, SK1, byResource, PK3, byResource, SK3, byResource);
    }

    private static Map<String, AttributeValue> ticketKeys(Ticket ticket) {
        var byTicket = stringAttribute(BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier()));
        var byResource = stringAttribute(BY_TYPE_PATTERN.formatted(RESOURCE, ticket.resourceIdentifier()));
        return Map.of(PK1, byResource, SK1, byTicket, PK2, byTicket, SK2, byTicket, PK3, byTicket, SK3, byTicket);
    }

    private static Map<String, AttributeValue> messageKeys(Message message) {
        var byMessage = stringAttribute(BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier()));
        var byResource = stringAttribute(BY_TYPE_PATTERN.formatted(RESOURCE, message.resourceIdentifier()));
        var byTicket = stringAttribute(BY_TYPE_PATTERN.formatted(TICKET, message.ticketIdentifier()));
        return Map.of(PK1, byResource, SK1, byMessage, PK2, byTicket, SK2, byMessage, PK3, byMessage, SK3, byMessage);
    }

    private static Map<String, AttributeValue> fileKeys(FileEntry fileEntry) {
        var byResource = stringAttribute(BY_TYPE_PATTERN.formatted(RESOURCE, fileEntry.resourceIdentifier()));
        var byFile = stringAttribute(BY_TYPE_PATTERN.formatted(FILE, fileEntry.identifier()));
        return Map.of(PK1, byResource, SK1, byFile);
    }

    private static String partitionKey(Entity entity) {
        return switch (entity) {
            case Resource resource ->
                BY_CUSTOMER_PATTERN.formatted(RESOURCE, resource.customerIdentifier(), resource.username());
            case Ticket ticket -> BY_CUSTOMER_PATTERN.formatted(TICKET, ticket.customerIdentifier(), ticket.username());
            case Message message ->
                BY_CUSTOMER_PATTERN.formatted(MESSAGE, message.customerIdentifier(), message.username());
            case LogEntry logEntry -> BY_TYPE_PATTERN.formatted(RESOURCE, logEntry.resourceIdentifier());
            case FileEntry fileEntry -> BY_TYPE_PATTERN.formatted(FILE, fileEntry.identifier());
            default -> throw new IllegalStateException(UNSUPPORTED_ENTITY.formatted(entity));
        };
    }

    private static String sortKey(Entity entity) {
        return switch (entity) {
            case Resource resource -> BY_TYPE_PATTERN.formatted(RESOURCE, resource.identifier());
            case Ticket ticket -> BY_TYPE_PATTERN.formatted(TICKET, ticket.identifier());
            case Message message -> BY_TYPE_PATTERN.formatted(MESSAGE, message.identifier());
            case LogEntry logEntry -> BY_TYPE_PATTERN.formatted(LOG_ENTRY, logEntry.identifier());
            case FileEntry fileEntry -> BY_TYPE_PATTERN.formatted(FILE, fileEntry.identifier());
            default -> throw new IllegalStateException(UNSUPPORTED_ENTITY.formatted(entity));
        };
    }

    private static String toJson(Entity entity) {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(entity);
        } catch (Exception exception) {
            throw new IllegalStateException(SERIALIZATION_FAILED, exception);
        }
    }
}
