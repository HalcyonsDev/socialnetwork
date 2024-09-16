package com.halcyon.mediaservice.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "is_like")
    private boolean isLike;

    @Column(name = "owner_id")
    private long ownerId;

    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    @JsonManagedReference
    private Post post;

    public Rating(boolean isLike, long ownerId, Post post) {
        this.isLike = isLike;
        this.ownerId = ownerId;
        this.post = post;
    }
}
