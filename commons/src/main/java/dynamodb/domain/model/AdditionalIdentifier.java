package dynamodb.domain.model;

public record AdditionalIdentifier(String type, String value) {

    public static final String CRISTIN = "Cristin";
    public static final String SCOPUS = "Scopus";

    public static AdditionalIdentifier ofCristin(String value) {
        return new AdditionalIdentifier(CRISTIN, value);
    }

    public static AdditionalIdentifier ofScopus(String value) {
        return new AdditionalIdentifier(SCOPUS, value);
    }

}
