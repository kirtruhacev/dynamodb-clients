package dynamodb.domain;

import dynamodb.domain.model.Entity;
import dynamodb.domain.model.FileEntry;
import dynamodb.domain.model.LogEntry;
import dynamodb.domain.model.Message;
import dynamodb.domain.model.Resource;
import dynamodb.domain.model.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Repository {

    void save(Entity entity);

    void save(Entity... entities);

    void updateVersion(Entity entity);

    Optional<Resource> getResourceByIdentifier(UUID identifier);

    Optional<Ticket> getTicketByIdentifier(UUID identifier);

    Optional<Message> getMessageByIdentifier(UUID identifier);

    Optional<Ticket> getTicketWithMessagesByTicketIdentifier(UUID identifier);

    List<Ticket> listTicketsByResourceIdentifier(UUID identifier);

    List<Ticket> listTicketsWithMessagesByResourceIdentifier(UUID identifier);

    List<LogEntry> listLogEntriesByResourceIdentifier(UUID identifier);

    List<FileEntry> listFilesByResourceIdentifier(UUID identifier);
}
