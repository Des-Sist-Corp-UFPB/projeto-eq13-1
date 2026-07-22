package br.ufpb.dsc.jobhub.domain;

public enum JobLocationType {
    REMOTE("Remota"),
    HYBRID_PB("Híbrida na Paraíba"),
    PRESENTIAL_PB("Presencial na Paraíba");

    private final String label;

    JobLocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
