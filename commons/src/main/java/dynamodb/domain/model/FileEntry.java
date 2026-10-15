package dynamodb.domain.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record FileEntry(UUID identifier, UUID resourceIdentifier, Instant modifiedDate) implements Entity {

}
