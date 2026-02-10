package net.devstudy.resume.profile.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionsVisibilityForm {
    @NotNull
    private Boolean visibleToConnections;
}
