package com.halcyon.mediaservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "comments")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "content")
    private String content;

    @Column(name = "author_email")
    private String authorEmail;

    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    @JsonManagedReference
    private Post post;

    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    @JsonManagedReference
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    @JsonBackReference
    private List<Comment> replies;
}
