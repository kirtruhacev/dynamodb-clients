package dynamodb.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PublicationChannel(UUID identifier, UUID resourceIdentifier, Instant modifiedDate) implements Entity {

}
