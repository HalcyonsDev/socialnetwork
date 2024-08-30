package com.halcyon.userservice.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "strikes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Strike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "cause")
    private String cause;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    @JsonManagedReference
    private User owner;

    @ManyToOne
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    @JsonManagedReference
    private User target;

    public Strike(String cause, User owner, User target) {
        this.cause = cause;
        this.owner = owner;
        this.target = target;
    }
}
