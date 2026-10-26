package in.gov.aicte.platform.compliance.dto;

import in.gov.aicte.platform.compliance.ComplianceObligation;
import java.time.LocalDate;

public record ComplianceResponse(
        String id,
        Long backendId,
        String obligationReference,
        Long institutionId,
        String institutionalIdentifier,
        String institutionName,
        String category,
        String requirement,
        String aphClause,
        LocalDate dueDate,
        String status,
        String urgency,
        String evidenceFile,
        String evidenceHash,
        String evidenceNotes,
        LocalDate submittedDate,
        String submittedBy,
        String reviewer,
        String verificationOutcome,
        String verificationRemarks,
        LocalDate verifiedDate,
        String verifiedBy
) {
    public static ComplianceResponse from(ComplianceObligation obl) {
        String instIdent = (obl.getInstitution() != null) ? obl.getInstitution().getInstitutionalIdentifier() : null;
        String instName = (obl.getInstitution() != null) ? obl.getInstitution().getName() : null;
        Long instId = (obl.getInstitution() != null) ? obl.getInstitution().getId() : null;

        return new ComplianceResponse(
                obl.getObligationReference(),
                obl.getId(),
                obl.getObligationReference(),
                instId,
                instIdent,
                instName,
                obl.getCategory(),
                obl.getRequirement(),
                obl.getAphClause(),
                obl.getDueDate(),
                obl.getStatus(),
                obl.getUrgency(),
                obl.getEvidenceFile(),
                obl.getEvidenceHash(),
                obl.getEvidenceNotes(),
                obl.getSubmittedDate(),
                obl.getSubmittedBy(),
                obl.getReviewer(),
                obl.getVerificationOutcome(),
                obl.getVerificationRemarks(),
                obl.getVerifiedDate(),
                obl.getVerifiedBy()
        );
    }
}
