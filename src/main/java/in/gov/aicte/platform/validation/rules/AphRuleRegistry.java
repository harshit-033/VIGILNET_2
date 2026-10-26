package in.gov.aicte.platform.validation.rules;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AphRuleRegistry {

    private final List<AphRuleDefinition> rules;
    private final Map<String, AphRuleDefinition> rulesById;

    public AphRuleRegistry() {
        this.rules = List.of(
                new AphRuleDefinition(
                        "RULE-LAND-01",
                        "Minimum Contiguous Land Area & Title Deed Ownership",
                        "APH 2026-27 Table 1.1 Norms",
                        "STATUTORY_MANDATORY",
                        "DETERMINISTIC_RULE",
                        "land_details",
                        List.of("land_details", "land_area", "land_title_deed"),
                        false,
                        "Verifies contiguous land possession of at least 2.5/5.0 acres based on urban/rural classification."
                ),
                new AphRuleDefinition(
                        "RULE-BUILTUP-02",
                        "Instructional and Administrative Built-up Area Norms",
                        "APH 2026-27 Table 1.2 Norms",
                        "STATUTORY_MANDATORY",
                        "DETERMINISTIC_RULE",
                        "builtup_area",
                        List.of("builtup_area", "land_details", "building_plan_approval"),
                        false,
                        "Verifies built-up area sufficiency relative to sanctioned programs and land parcel FAR."
                ),
                new AphRuleDefinition(
                        "RULE-FACULTY-03",
                        "Student-Faculty Cadre Ratio & Sanctioned Cadre Distribution",
                        "APH 2026-27 Table 2.1 Norms",
                        "STATUTORY_MANDATORY",
                        "DETERMINISTIC_RULE",
                        "faculty_roster",
                        List.of("faculty_roster", "student_intake"),
                        false,
                        "Enforces 1:15 or 1:20 SFR and 1:2:6 Professor : Associate Professor : Assistant Professor cadre ratio."
                ),
                new AphRuleDefinition(
                        "RULE-FIRE-NOC-04",
                        "State Fire Service Safety NOC & Periodic Endorsement",
                        "APH 2026-27 Clause 3.4 Norms",
                        "STATUTORY_MANDATORY",
                        "DETERMINISTIC_RULE",
                        "fire_safety_noc",
                        List.of("fire_safety_noc", "fire_safety_expiry_date"),
                        true, // Time-dependent! Forces re-evaluation if expiring/expired
                        "Verifies active, non-expired statutory Fire Safety Certificate issued by competent government authority."
                ),
                new AphRuleDefinition(
                        "RULE-BUILDING-OCCUPANCY-05",
                        "Occupancy / Completion Certificate from Local Competent Authority",
                        "APH 2026-27 Clause 3.2 Norms",
                        "STATUTORY_MANDATORY",
                        "DETERMINISTIC_RULE",
                        "occupancy_certificate",
                        List.of("occupancy_certificate", "builtup_area"),
                        false,
                        "Mandatory occupancy/completion certificate for all academic and laboratory structures."
                ),
                new AphRuleDefinition(
                        "RULE-LABORATORY-06",
                        "Mandatory Laboratory Equipment & AICTE Computing Facilities",
                        "APH 2026-27 Clause 4.2 Norms",
                        "STATUTORY_MANDATORY",
                        "DETERMINISTIC_RULE",
                        "lab_infrastructure",
                        List.of("lab_infrastructure", "student_intake"),
                        false,
                        "Verifies prescribed terminal counts, licensed system software, and laboratory kits."
                ),
                new AphRuleDefinition(
                        "RULE-LIBRARY-07",
                        "Library Resources & National/International Journal Subscriptions",
                        "APH 2026-27 Clause 4.5 Norms",
                        "ADVISORY_RECOMMENDED",
                        "DETERMINISTIC_RULE",
                        "library_resources",
                        List.of("library_resources"),
                        false,
                        "Soft advisory check for library volume count and e-journal subscription compliance."
                ),
                new AphRuleDefinition(
                        "RULE-AI-READINESS-08",
                        "Automated Scrutiny Risk Index & Deficiency Assessment",
                        "APH 2026-27 Appendix 6 Guidelines",
                        "ADVISORY_RECOMMENDED",
                        "AI_ASSISTED_ADVISORY",
                        "application_dossier",
                        List.of("land_details", "faculty_roster", "lab_infrastructure", "fire_safety_noc"),
                        false,
                        "AI-assisted readiness indicator. Non-binding preliminary score to assist scrutiny officers."
                )
        );

        this.rulesById = rules.stream().collect(Collectors.toMap(AphRuleDefinition::ruleId, Function.identity()));
    }

    public List<AphRuleDefinition> getAllRules() {
        return Collections.unmodifiableList(rules);
    }

    public Optional<AphRuleDefinition> getRuleById(String ruleId) {
        return Optional.ofNullable(rulesById.get(ruleId));
    }
}
