package dynamodb.domain;

import static dynamodb.domain.model.Status.DRAFT;
import static java.util.UUID.randomUUID;
import dynamodb.domain.model.AdditionalIdentifier;
import dynamodb.domain.model.EntityDescription;
import dynamodb.domain.model.FileEntry;
import dynamodb.domain.model.LogEntry;
import dynamodb.domain.model.Message;
import dynamodb.domain.model.PublicationContext;
import dynamodb.domain.model.PublicationInstance;
import dynamodb.domain.model.Reference;
import dynamodb.domain.model.Resource;
import dynamodb.domain.model.Ticket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class TestUtil {

    public static Resource randomResource(UUID customerIdentifier) {
        return new Resource(randomUUID(), customerIdentifier, randomString(), DRAFT, randomEntityDescription(),
                            Instant.now(), List.of(AdditionalIdentifier.ofCristin(randomString()),
                                                   AdditionalIdentifier.ofScopus(randomString())));
    }

    public static EntityDescription randomEntityDescription() {
        return new EntityDescription(randomString(), randomString(),
                                     randomReference());
    }

    private static Reference randomReference() {
        return new Reference(new PublicationContext(randomString()),
                             new PublicationInstance(randomString(), randomString()), randomString());
    }

    public static Ticket randomTicket(UUID resourceIdentifier, UUID customerIdentifier) {
        return new Ticket(randomUUID(), resourceIdentifier, customerIdentifier, randomString(), List.of(), Instant.now());
    }

    public static Message randomMessage(UUID ticketIdentifier, UUID resourceIdentifier) {
        return new Message(randomUUID(), ticketIdentifier, resourceIdentifier, randomUUID(), randomString(), Instant.now());
    }

    public static LogEntry randomLogEntry(UUID resourceIdentifier) {
        return new LogEntry(randomUUID(), resourceIdentifier, Instant.now());
    }

    public static FileEntry randomFileEntry(UUID resourceIdentifier) {
        return new FileEntry(randomUUID(), resourceIdentifier, Instant.now());
    }

    public static String randomString() {
        byte[] array = new byte[10];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
