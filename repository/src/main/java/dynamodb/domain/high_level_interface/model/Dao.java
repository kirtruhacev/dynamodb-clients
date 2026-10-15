package dynamodb.domain.high_level_interface.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = ResourceDao.class), @JsonSubTypes.Type(TicketDao.class),
    @JsonSubTypes.Type(value = MessageDao.class), @JsonSubTypes.Type(value = LogEntryDao.class),
    @JsonSubTypes.Type(value = FileEntryDao.class)})
public abstract class Dao {

    private UUID identifier;
    private Instant modifiedDate;
    private String version;

    public Dao() {
    }

    public Dao(UUID identifier, Instant modifiedDate) {
        this.identifier = identifier;
        this.modifiedDate = modifiedDate;
    }

    @JsonProperty("type")
    public abstract String getType();

    public UUID getIdentifier() {
        return identifier;
    }

    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
