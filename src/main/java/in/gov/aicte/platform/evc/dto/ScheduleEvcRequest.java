package in.gov.aicte.platform.evc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ScheduleEvcRequest(
        @NotNull(message = "Visit date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate visitDate,

        String coordinator,
        String inspectionType,
        List<String> experts,
        String actorName,
        String actorRole
) {
}
