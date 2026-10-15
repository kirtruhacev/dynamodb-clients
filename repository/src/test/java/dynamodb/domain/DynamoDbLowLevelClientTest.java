package dynamodb.domain;

import static dynamodb.domain.DynamoDBLocal.dynamoDBLocal;
import static dynamodb.domain.TestUtil.randomFileEntry;
import static dynamodb.domain.TestUtil.randomLogEntry;
import static dynamodb.domain.TestUtil.randomMessage;
import static dynamodb.domain.TestUtil.randomResource;
import static dynamodb.domain.TestUtil.randomTicket;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import dynamodb.domain.low_level_interface.DynamoDbLowLevelClient;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

class DynamoDbLowLevelClientTest {

    private static final String MY_TABLE = DatabaseConstants.TABLE_NAME;
    public static final AmazonDynamoDBLocal database = DynamoDBEmbedded.create();
    private DynamoDbClient client;
    private Repository repository;

    @BeforeEach
    void setUp() {
        client = dynamoDBLocal(database, MY_TABLE);
        repository = new DynamoDbLowLevelClient(client);
    }

    @AfterEach
    void tearDown() {
        client.deleteTable(DeleteTableRequest.builder().tableName(MY_TABLE).build());
    }

    @Test
    void shouldFetchPublicationByIdentifier() {
        var resource = randomResource(randomUUID());
        repository.save(resource);

        var persistedResource = repository.getResourceByIdentifier(resource.identifier()).orElseThrow();

        assertEquals(resource, persistedResource);
    }

    @Test
    void shouldFetchTicketByIdentifier() {
        var ticket = randomTicket(randomUUID(), randomUUID());
        repository.save(ticket);

        var persistedTicket = repository.getTicketByIdentifier(ticket.identifier()).orElseThrow();

        assertEquals(ticket, persistedTicket);
    }

    @Test
    void shouldFetchMessageByIdentifier() {
        var message = randomMessage(randomUUID(), randomUUID());
        repository.save(message);

        var persistedMessage = repository.getMessageByIdentifier(message.identifier()).orElseThrow();

        assertEquals(message, persistedMessage);
    }

    @Test
    void shouldListAllTicketsForPublication() {
        var resourceIdentifier = randomUUID();
        var ticket1 = randomTicket(resourceIdentifier, randomUUID());
        var ticket2 = randomTicket(resourceIdentifier, randomUUID());
        repository.save(ticket1, ticket2);

        var tickets = repository.listTicketsByResourceIdentifier(resourceIdentifier);

        assertEquals(2, tickets.size());
        assertTrue(tickets.containsAll(List.of(ticket1, ticket2)));
    }

    @Test
    void shouldFetchTicketWithMessagesByTicketIdentifier() {
        var resourceIdentifier = randomUUID();
        var ticket = randomTicket(resourceIdentifier, randomUUID());

        var message1 = randomMessage(ticket.identifier(), resourceIdentifier);
        var message2 = randomMessage(ticket.identifier(), resourceIdentifier);
        repository.save(ticket, message1, message2);

        var ticketWithMessages = repository.getTicketWithMessagesByTicketIdentifier(ticket.identifier())
                                     .orElseThrow();

        assertEquals(2, ticketWithMessages.messages().size());
        assertTrue(ticketWithMessages.messages().containsAll(List.of(message1, message2)));
    }

    @Test
    void shouldFetchTicketWithMessagesByResourceIdentifier() {
        var resource = randomResource(randomUUID());
        var resourceIdentifier = resource.identifier();
        var ticket1 = randomTicket(resourceIdentifier, randomUUID());
        var ticket2 = randomTicket(resourceIdentifier, randomUUID());

        var message1 = randomMessage(ticket1.identifier(), resourceIdentifier);
        var message2 = randomMessage(ticket2.identifier(), resourceIdentifier);

        repository.save(resource, ticket1, message1, ticket2, message2);

        var tickets = repository.listTicketsWithMessagesByResourceIdentifier(resourceIdentifier);

        var actualTicket1 = tickets.stream()
                                .filter(ticket -> ticket.identifier().equals(ticket1.identifier()))
                                .findFirst()
                                .orElseThrow();

        assertEquals(2, tickets.size());
        assertTrue(actualTicket1.messages().contains(message1));
    }

    @Test
    void shouldListLogEntriesForResource() {
        var resourceIdentifier = randomUUID();
        var logEntry1 = randomLogEntry(resourceIdentifier);
        var logEntry2 = randomLogEntry(resourceIdentifier);

        repository.save(logEntry1, logEntry2);

        var logEntries = repository.listLogEntriesByResourceIdentifier(resourceIdentifier);

        assertEquals(2, logEntries.size());
        assertTrue(logEntries.containsAll(List.of(logEntry1, logEntry2)));
    }

    @Test
    void shouldListFileEntriesForResource() {
        var resourceIdentifier = randomUUID();
        var fileEntry1 = randomFileEntry(resourceIdentifier);
        var fileEntry2 = randomFileEntry(resourceIdentifier);

        repository.save(fileEntry1, fileEntry2);

        var fileEntries = repository.listFilesByResourceIdentifier(resourceIdentifier);

        assertEquals(2, fileEntries.size());
        assertTrue(fileEntries.containsAll(List.of(fileEntry1, fileEntry2)));
    }

    @Test
    void shouldStampVersionWithoutOverwritingStoredEntity() {
        var resource = randomResource(randomUUID());
        repository.save(resource);

        repository.updateVersion(resource);

        var storedItem = client.scan(ScanRequest.builder().tableName(MY_TABLE).build()).items().getFirst();
        assertNotNull(storedItem.get("version"));
        assertEquals(resource, repository.getResourceByIdentifier(resource.identifier()).orElseThrow());
    }
}
