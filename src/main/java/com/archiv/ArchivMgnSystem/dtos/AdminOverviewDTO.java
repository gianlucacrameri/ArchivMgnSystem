package com.archiv.ArchivMgnSystem.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Was auf einen Admin wartet - für die Zähler in der Navigation. */
@Data
@AllArgsConstructor
public class AdminOverviewDTO {
    private long pendingRequests;
    private long pendingRegistrations;
}
