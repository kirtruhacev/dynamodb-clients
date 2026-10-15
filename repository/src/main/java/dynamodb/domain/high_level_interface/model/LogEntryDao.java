package dynamodb.domain.high_level_interface.model;

import static dynamodb.domain.DatabaseConstants.GSI1;
import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.SK0;
import static dynamodb.domain.DatabaseConstants.SK1;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import dynamodb.domain.model.LogEntry;
import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@JsonTypeName("LogEntry")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@DynamoDbBean
public class LogEntryDao extends Dao {

    private static final String TYPE = "LogEntry";
    private UUID resourceIdentifier;

    public LogEntryDao() {
    }

    public LogEntryDao(UUID identifier, UUID resourceIdentifier, Instant modifiedDate) {
        super(identifier, modifiedDate);
        this.resourceIdentifier = resourceIdentifier;
    }

    public static LogEntryDao fromLogEntry(LogEntry logEntry) {
        return new LogEntryDao(logEntry.identifier(), logEntry.resourceIdentifier(), logEntry.modifiedDate());
    }

    public LogEntry toLogEntry() {
        return new LogEntry(getIdentifier(), getResourceIdentifier(), getModifiedDate());
    }

    @Override
    public String getType() {
        return "LogEntry";
    }

    public void setType(String type) {

    }

    @DynamoDbAttribute(PK0)
    @DynamoDbPartitionKey
    @JsonProperty(PK0)
    public String getPartitionKey() {
        return "%s:%s".formatted(ResourceDao.TYPE, getResourceIdentifier());
    }

    public void setPartitionKey(String partitionKey) {

    }

    @DynamoDbAttribute(SK0)
    @DynamoDbSortKey
    @JsonProperty(SK0)
    public String getSortKey() {
        return "%s:%s".formatted(TYPE, getIdentifier());
    }

    public void setSortKey(String sortKey) {

    }

    @DynamoDbSecondaryPartitionKey(indexNames = {GSI1})
    @DynamoDbAttribute(PK1)
    @JsonProperty(PK1)
    public String getGsi1PartitionKey() {
        return "%s:%s".formatted(ResourceDao.TYPE, getResourceIdentifier());
    }

    public void setGsi1PartitionKey(String value) {

    }

    @DynamoDbSecondarySortKey(indexNames = {GSI1})
    @DynamoDbAttribute(SK1)
    @JsonProperty(SK1)
    public String getGsi1SortKey() {
        return "%s:%s".formatted(TYPE, getIdentifier());
    }

    public void setGsi1SortKey(String value) {

    }

    public UUID getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(UUID resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }
}
