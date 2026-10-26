package in.gov.aicte.platform.institution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institutional_identifier", nullable = false, unique = true, length = 64)
    private String institutionalIdentifier;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Institution() {
    }

    public Institution(String institutionalIdentifier, String name) {
        this.institutionalIdentifier = institutionalIdentifier;
        this.name = name;
    }

    public Institution(String institutionalIdentifier, String name, String region) {
        this.institutionalIdentifier = institutionalIdentifier;
        this.name = name;
        this.region = region;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstitutionalIdentifier() {
        return institutionalIdentifier;
    }

    public void setInstitutionalIdentifier(String institutionalIdentifier) {
        this.institutionalIdentifier = institutionalIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Institution that = (Institution) o;
        return Objects.equals(institutionalIdentifier, that.institutionalIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(institutionalIdentifier);
    }
}
