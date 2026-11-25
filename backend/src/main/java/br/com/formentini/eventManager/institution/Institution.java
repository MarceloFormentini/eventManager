
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

    @Column(length = 300)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 254)
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Institution() {
    }

    public Institution(TypeInstitution typeInstitution, String name) {
        this(typeInstitution, name, null, null, null);
    }

    public Institution(
            TypeInstitution typeInstitution,
            String name,
            String address,
            String phone,
            String email
    ) {
        this.name = name;
        this.typeInstitution = typeInstitution;
        this.address = address;
        this.phone = phone;
        this.email = email;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
