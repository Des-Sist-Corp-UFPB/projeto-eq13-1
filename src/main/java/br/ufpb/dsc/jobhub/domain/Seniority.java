package br.ufpb.dsc.jobhub.domain;

public enum Seniority {
    INTERNSHIP("Estágio"),
    JUNIOR("Júnior"),
    MID_LEVEL("Pleno"),
    SENIOR("Sênior"),
    LEAD("Lead");

    private final String label;

    Seniority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
