package in.gov.aicte.platform.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInstitutionRequest(
        @NotBlank(message = "Institutional identifier is required")
        @Size(max = 64, message = "Institutional identifier must not exceed 64 characters")
        String institutionalIdentifier,

        @NotBlank(message = "Institution name is required")
        @Size(max = 255, message = "Institution name must not exceed 255 characters")
        String name
) {
    public CreateInstitutionRequest {
        if (institutionalIdentifier != null) {
            institutionalIdentifier = institutionalIdentifier.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }
}
