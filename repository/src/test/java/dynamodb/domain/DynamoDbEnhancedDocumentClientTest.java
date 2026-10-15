package dynamodb.domain;

import static dynamodb.domain.DatabaseConstants.TABLE_NAME;
import static dynamodb.domain.DynamoDBLocal.dynamoDBLocal;
import static dynamodb.domain.TestUtil.randomFileEntry;
import static dynamodb.domain.TestUtil.randomLogEntry;
import static dynamodb.domain.TestUtil.randomMessage;
import static dynamodb.domain.TestUtil.randomResource;
import static dynamodb.domain.TestUtil.randomTicket;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import dynamodb.domain.document_interface.DynamoDbEnhancedDocumentClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

class DynamoDbEnhancedDocumentClientTest {

    private static final String MY_TABLE = TABLE_NAME;
    public static final AmazonDynamoDBLocal database = DynamoDBEmbedded.create();
    private DynamoDbClient client;
    private Repository repository;

    @BeforeEach
    void setUp() {
        this.client = dynamoDBLocal(database, MY_TABLE);
        repository = new DynamoDbEnhancedDocumentClient(client);
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

        assertThat(persistedResource)
            .usingRecursiveComparison()
            .ignoringFields("modifiedDate")
            .isEqualTo(resource);
    }

    @Test
    void shouldFetchTicketByIdentifier() {
        var ticket = randomTicket(randomUUID(), randomUUID());
        repository.save(ticket);

        var persistedTicket = repository.getTicketByIdentifier(ticket.identifier()).orElseThrow();

        assertThat(persistedTicket)
            .usingRecursiveComparison()
            .ignoringFields("modifiedDate")
            .isEqualTo(ticket);
    }

    @Test
    void shouldFetchMessageByIdentifier() {
        var message = randomMessage(randomUUID(), randomUUID());
        repository.save(message);

        var persistedMessage = repository.getMessageByIdentifier(message.identifier()).orElseThrow();

        assertThat(persistedMessage)
            .usingRecursiveComparison()
            .ignoringFields("modifiedDate")
            .isEqualTo(message);
    }

    @Test
    void shouldListAllTicketsForPublication() {
        var resourceIdentifier = randomUUID();
        var ticket1 = randomTicket(resourceIdentifier, randomUUID());
        var ticket2 = randomTicket(resourceIdentifier, randomUUID());
        repository.save(ticket1, ticket2);

        var tickets = repository.listTicketsByResourceIdentifier(resourceIdentifier);

        assertEquals(2, tickets.size());
        assertTrue(tickets.contains(ticket1));
        assertTrue(tickets.contains(ticket2));
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

        assertTrue(ticketWithMessages.messages().contains(message1));
        assertTrue(ticketWithMessages.messages().contains(message2));
    }

    @Test
    void shouldFetchTicketWithMessagesByResourceIdentifier() {
        var resource = randomResource(randomUUID());
        var resourceIdentifier = resource.identifier();
        var ticket1 = randomTicket(resourceIdentifier, randomUUID());
        var ticket2 = randomTicket(resourceIdentifier, randomUUID());

        var message1 = randomMessage(ticket1.identifier(), resourceIdentifier);
        var message2 = randomMessage(ticket1.identifier(), resourceIdentifier);

        var message3 = randomMessage(ticket2.identifier(), resourceIdentifier);
        var message4 = randomMessage(ticket2.identifier(), resourceIdentifier);

        repository.save(resource,
                        ticket1,
                        message1,
                        message2,
                        ticket2,
                        message3,
                        message4);

        var tickets = repository.listTicketsWithMessagesByResourceIdentifier(resourceIdentifier);

        assertEquals(2, tickets.size());

        var actualTicket1 = tickets.stream()
                                .filter(t -> t.identifier().equals(ticket1.identifier()))
                                .findFirst()
                                .orElseThrow();

        assertTrue(actualTicket1.messages().contains(message1));
        assertTrue(actualTicket1.messages().contains(message2));

        var actualTicket2 =
            tickets.stream().filter(t -> t.identifier().equals(ticket2.identifier())).findFirst().orElseThrow();

        assertTrue(actualTicket2.messages().contains(message3));
        assertTrue(actualTicket2.messages().contains(message4));
    }

    @Test
    void shouldListLogEntriesForResource() {
        var resourceIdentifier = randomUUID();
        var logEntry1 = randomLogEntry(resourceIdentifier);
        var logEntry2 = randomLogEntry(resourceIdentifier);

        repository.save(logEntry1, logEntry2);

        var logEntries = repository.listLogEntriesByResourceIdentifier(resourceIdentifier);

        assertEquals(2, logEntries.size());
        assertTrue(logEntries.contains(logEntry1));
        assertTrue(logEntries.contains(logEntry2));
    }

    @Test
    void shouldListFileEntriesForResource() {
        var resourceIdentifier = randomUUID();
        var fileEntry1 = randomFileEntry(resourceIdentifier);
        var fileEntry2 = randomFileEntry(resourceIdentifier);

        repository.save(fileEntry1, fileEntry2);

        var fileEntries = repository.listFilesByResourceIdentifier(resourceIdentifier);

        assertEquals(2, fileEntries.size());
        assertTrue(fileEntries.contains(fileEntry1));
        assertTrue(fileEntries.contains(fileEntry2));
    }

    @Test
    void shouldUpdateVersionForDaoWithoutOverridingDocument() {
        var resource = randomResource(randomUUID());
        repository.save(resource);

        var itemBeforeUpdate = client.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).items().getFirst();

        assertNull(itemBeforeUpdate.get("version"));

        repository.updateVersion(resource);

        var fetchedItem = client.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).items().getFirst();

        assertNotNull(fetchedItem.get("version"));
        assertEquals(resource, repository.getResourceByIdentifier(resource.identifier()).orElseThrow());
    }
}
