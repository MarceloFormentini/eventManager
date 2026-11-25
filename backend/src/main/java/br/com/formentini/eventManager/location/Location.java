
package br.com.formentini.eventManager.location;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

import br.com.formentini.eventManager.institution.Institution;

@Entity
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Location() {
    }

    public Location(Institution institution, String name) {
        this(institution, name, 0, null);
    }

    public Location(
            Institution institution,
            String name,
            Integer capacity,
            String description
    ) {
        this.name = name;
        this.institution = institution;
        this.capacity = capacity == null ? 0 : capacity;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity == null ? 0 : capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
