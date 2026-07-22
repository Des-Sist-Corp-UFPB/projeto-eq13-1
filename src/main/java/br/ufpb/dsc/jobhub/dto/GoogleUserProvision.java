package br.ufpb.dsc.jobhub.dto;

import br.ufpb.dsc.jobhub.domain.AppUser;

public record GoogleUserProvision(AppUser user, boolean created) {
}
