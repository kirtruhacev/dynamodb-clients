package dynamodb.domain.high_level_interface.model;

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import dynamodb.domain.model.Message;
import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@JsonTypeName("Message")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@DynamoDbBean
public class MessageDao extends Dao {

    private static final String TYPE = "Message";
    private UUID resourceIdentifier;
    private UUID ticketIdentifier;
    private UUID customerIdentifier;
    private String username;

    public MessageDao() {
        super();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public MessageDao(UUID identifier, UUID ticketIdentifier, UUID resourceIdentifier, UUID customerIdentifier,
                      String username, Instant modifiedDate) {
        super(identifier, modifiedDate);
        this.ticketIdentifier = ticketIdentifier;
        this.resourceIdentifier = resourceIdentifier;
        this.customerIdentifier = customerIdentifier;
        this.username = username;
    }

    public static MessageDao fromMessage(Message message) {
        return new MessageDao(message.identifier(), message.ticketIdentifier(), message.resourceIdentifier(),
                              message.customerIdentifier(), message.username(), message.modifiedDate());
    }

    public static String gsi3PartitionValue(UUID identifier) {
        return "%s:%s".formatted(TYPE, identifier);
    }

    public Message toMessage() {
        return new Message(getIdentifier(), ticketIdentifier, resourceIdentifier, customerIdentifier, username, getModifiedDate());
    }

    public void setType(String value) {

    }

    public UUID getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(UUID ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }

    public UUID getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(UUID resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public UUID getCustomerIdentifier() {
        return customerIdentifier;
    }

    public void setCustomerIdentifier(UUID customerIdentifier) {
        this.customerIdentifier = customerIdentifier;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDbAttribute(PK0)
    @DynamoDbPartitionKey
    @JsonProperty(PK0)
    public String getPartitionKey() {
        return "%s:%s:%s".formatted(TYPE, customerIdentifier, username);
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

    @DynamoDbSecondaryPartitionKey(indexNames = {GSI2})
    @DynamoDbAttribute(PK2)
    @JsonProperty(PK2)
    public String getGsi2PartitionKey() {
        return "%s:%s".formatted(TicketDao.TYPE, getTicketIdentifier());
    }

    public void setGsi2PartitionKey(String value) {

    }

    @DynamoDbSecondarySortKey(indexNames = {GSI2})
    @DynamoDbAttribute(SK2)
    @JsonProperty(SK2)
    public String getGsi2SortKey() {
        return "%s:%s".formatted(TYPE, getIdentifier());
    }

    public void setGsi2SortKey(String value) {

    }

    @DynamoDbSecondaryPartitionKey(indexNames = {GSI3})
    @DynamoDbAttribute(PK3)
    @JsonProperty(PK3)
    public String getGsi3PartitionKey() {
        return "%s:%s".formatted(TYPE, getIdentifier());
    }

    public void setGsi3PartitionKey(String value) {

    }

    @DynamoDbSecondarySortKey(indexNames = {GSI3})
    @DynamoDbAttribute(SK3)
    @JsonProperty(SK3)
    public String getGsi3SortKey() {
        return "%s:%s".formatted(TYPE, getIdentifier());
    }

    public void setGsi3SortKey(String value) {

    }
}
