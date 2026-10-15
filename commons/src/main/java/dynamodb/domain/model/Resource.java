package dynamodb.domain.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record Resource(UUID identifier, UUID customerIdentifier, String username, Status status,
                       EntityDescription entityDescription, Instant modifiedDate,
                       List<AdditionalIdentifier> additionalIdentifiers) implements Entity {

}
