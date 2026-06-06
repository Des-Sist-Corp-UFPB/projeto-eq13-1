package br.ufpb.dsc.jobhub.domain;

public enum Seniority {
    INTERNSHIP("Estagio"),
    JUNIOR("Junior"),
    MID_LEVEL("Pleno"),
    SENIOR("Senior"),
    LEAD("Lead");

    private final String label;

    Seniority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
