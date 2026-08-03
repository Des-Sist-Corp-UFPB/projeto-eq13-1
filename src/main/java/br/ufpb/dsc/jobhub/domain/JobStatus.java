package br.ufpb.dsc.jobhub.domain;

public enum JobStatus {
    PENDING("Pendente"),
    PUBLISHED("Publicada"),
    ARCHIVED("Arquivada");

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
