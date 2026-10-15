package dynamodb.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(Resource.class),
    @JsonSubTypes.Type(Ticket.class),
    @JsonSubTypes.Type(FileEntry.class),
    @JsonSubTypes.Type(Message.class),
    @JsonSubTypes.Type(LogEntry.class),
})
public sealed interface Entity permits FileEntry, LogEntry, Message, PublicationChannel, Resource, Ticket {

    UUID identifier();
}
