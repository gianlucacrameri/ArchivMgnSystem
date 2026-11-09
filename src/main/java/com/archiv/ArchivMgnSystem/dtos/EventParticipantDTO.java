package com.archiv.ArchivMgnSystem.dtos;

import com.archiv.ArchivMgnSystem.enums.ParticipantStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventParticipantDTO {

    private Long id;
    private Long userId;
    private String username;
    private ParticipantStatus status;
}
