package in.gov.aicte.platform.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotNull(message = "Institution ID is required")
        Long institutionId,

        @NotBlank(message = "Application reference is required")
        @Size(max = 64, message = "Application reference must not exceed 64 characters")
        String applicationReference,

        @NotBlank(message = "Application type is required")
        @Size(max = 64, message = "Application type must not exceed 64 characters")
        String applicationType,

        @NotBlank(message = "Academic year is required")
        @Size(max = 16, message = "Academic year must not exceed 16 characters")
        String academicYear
) {
    public CreateApplicationRequest {
        if (applicationReference != null) {
            applicationReference = applicationReference.trim();
        }
        if (applicationType != null) {
            applicationType = applicationType.trim();
        }
        if (academicYear != null) {
            academicYear = academicYear.trim();
        }
    }
}
