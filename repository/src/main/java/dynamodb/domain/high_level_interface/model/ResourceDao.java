package dynamodb.domain.high_level_interface.model;

import static dynamodb.domain.DatabaseConstants.GSI1;
import static dynamodb.domain.DatabaseConstants.GSI3;
import static dynamodb.domain.DatabaseConstants.PK0;
import static dynamodb.domain.DatabaseConstants.PK1;
import static dynamodb.domain.DatabaseConstants.PK3;
import static dynamodb.domain.DatabaseConstants.SK0;
import static dynamodb.domain.DatabaseConstants.SK1;
import static dynamodb.domain.DatabaseConstants.SK3;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import dynamodb.domain.model.Resource;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@JsonTypeName("Resource")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@DynamoDbBean
public class ResourceDao extends Dao {

    protected static final String TYPE = "Resource";
    private String username;
    private UUID customerIdentifier;
    private String data;

    public ResourceDao() {
    }

    public ResourceDao(UUID identifier, UUID customerIdentifier, String username, String data, Instant modifiedDate) {
        super(identifier, modifiedDate);
        this.customerIdentifier = customerIdentifier;
        this.username = username;
        this.data = data;
    }

    public static ResourceDao fromResource(Resource resource) {
        return new ResourceDao(resource.identifier(), resource.customerIdentifier(), resource.username(),
                               toString(resource), resource.modifiedDate());
    }

    public static String gsi3PartitionValue(UUID identifier) {
        return "%s:%s".formatted(TYPE, identifier);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void setType(String value) {

    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Resource toResource() {
        try {
            return JsonUtils.dtoObjectMapper.readValue(data, Resource.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        return "%s:%s".formatted(TYPE, getIdentifier());
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

    private static String toString(Resource resource) {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
