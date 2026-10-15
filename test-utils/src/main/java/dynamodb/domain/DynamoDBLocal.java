package dynamodb.domain;

import static java.util.Objects.isNull;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import java.util.ArrayList;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class DynamoDBLocal {

    public static DynamoDbClient dynamoDBLocal(AmazonDynamoDBLocal database, String tableName) {
        var client = database.dynamoDbClient();
        createTable(tableName, client);
        return client;
    }

    private static void createTable(String tableName, DynamoDbClient client) {
        client.createTable(CreateTableRequest.builder()
                               .tableName(tableName)
                               .provisionedThroughput(createProvisionedThroughput())
                               .attributeDefinitions(createAttribute(DatabaseConstants.PK0), createAttribute(
                                                         DatabaseConstants.SK0), createAttribute(DatabaseConstants.PK1),
                                                     createAttribute(DatabaseConstants.SK1), createAttribute(
                                       DatabaseConstants.PK2), createAttribute(DatabaseConstants.SK2),
                                                     createAttribute(DatabaseConstants.PK3), createAttribute(
                                       DatabaseConstants.SK3), createAttribute(DatabaseConstants.PK4),
                                                     createAttribute(DatabaseConstants.SK4))
                               .globalSecondaryIndexes(createGlobalSecondaryIndex(DatabaseConstants.GSI1, DatabaseConstants.PK1, DatabaseConstants.SK1),
                                                       createGlobalSecondaryIndex(DatabaseConstants.GSI2, DatabaseConstants.PK2, DatabaseConstants.SK2),
                                                       createGlobalSecondaryIndex(DatabaseConstants.GSI3, DatabaseConstants.PK3, DatabaseConstants.SK3),
                                                       createGlobalSecondaryIndex(DatabaseConstants.GSI4, DatabaseConstants.PK4, DatabaseConstants.SK4))
                               .keySchema(createKeySchemaElements(DatabaseConstants.PK0, DatabaseConstants.SK0))
                               .build());
    }

    private static GlobalSecondaryIndex createGlobalSecondaryIndex(String name, String partitionKey, String sortKey) {
        return GlobalSecondaryIndex.builder()
                   .indexName(name)
                   .provisionedThroughput(createProvisionedThroughput())
                   .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                   .keySchema(createKeySchemaElements(partitionKey, sortKey))
                   .build();
    }

    private static ArrayList<KeySchemaElement> createKeySchemaElements(String partitionKey, String sortKey) {
        var keySchemas = new ArrayList<KeySchemaElement>();
        createSchemaElement(KeyType.HASH, partitionKey).ifPresentOrElse(keySchemas::add, IllegalArgumentException::new);
        createSchemaElement(KeyType.RANGE, sortKey).ifPresent(keySchemas::add);
        return keySchemas;
    }

    private static ProvisionedThroughput createProvisionedThroughput() {
        return ProvisionedThroughput.builder().readCapacityUnits(1000L).writeCapacityUnits(1000L).build();
    }

    private static Optional<KeySchemaElement> createSchemaElement(KeyType keyType, String attributeName) {
        return isNullSortKey(keyType, attributeName) ? Optional.empty()
                   : Optional.of(KeySchemaElement.builder().keyType(keyType).attributeName(attributeName).build());
    }

    private static boolean isNullSortKey(KeyType keyType, String attributeName) {
        return KeyType.RANGE == keyType && isNull(attributeName);
    }

    private static AttributeDefinition createAttribute(String attributeName) {
        return AttributeDefinition.builder().attributeName(attributeName).attributeType(ScalarAttributeType.S).build();
    }
}
