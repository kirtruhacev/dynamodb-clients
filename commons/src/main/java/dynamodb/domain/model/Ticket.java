package dynamodb.domain.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record Ticket(UUID identifier, UUID resourceIdentifier, UUID customerIdentifier, String username,
                     List<Message> messages, Instant modifiedDate) implements Entity {

}
