package br.ufpb.dsc.jobhub.domain;

public enum JobLocationType {
    REMOTE("Remota"),
    PRESENTIAL_PB("Presencial na Paraiba");

    private final String label;

    JobLocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
