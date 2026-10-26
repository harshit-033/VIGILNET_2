package in.gov.aicte.platform.evc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record RescheduleEvcRequest(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate newVisitDate,

        String actorName,
        String actorRole
) {
}
