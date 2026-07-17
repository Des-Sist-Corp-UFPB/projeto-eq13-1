package br.ufpb.dsc.jobhub.domain;

public enum ThemePreference {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Escuro");

    private final String label;

    ThemePreference(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
