package dynamodb.domain.model;

public record Reference(PublicationContext publicationContext, PublicationInstance publicationInstance, String doi) {

}
