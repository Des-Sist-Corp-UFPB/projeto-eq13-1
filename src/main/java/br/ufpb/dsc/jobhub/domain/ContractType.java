package br.ufpb.dsc.jobhub.domain;

public enum ContractType {
    CLT("CLT"),
    PJ("PJ"),
    INTERNSHIP("Estagio"),
    FREELANCE("Freelance"),
    OTHER("Outro");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
