
package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "institution")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_institution_id", nullable = false)
    private TypeInstitution typeInstitution;

    @Column(nullable = false, length = 150)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Institution() {
    }

    public Institution(TypeInstitution typeInstitution, String name) {
        this.name = name;
        this.typeInstitution = typeInstitution;
    }

    public Long getId() {
        return id;
    }

    public TypeInstitution getTypeInstitution() {
        return typeInstitution;
    }

    public void setTypeInstitution(TypeInstitution typeInstitution) {
        this.typeInstitution = typeInstitution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
