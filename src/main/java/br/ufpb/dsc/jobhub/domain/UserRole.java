package br.ufpb.dsc.jobhub.domain;

public enum UserRole {
    ROLE_ADMIN("Administrador"),
    ROLE_USER("Usuário");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
