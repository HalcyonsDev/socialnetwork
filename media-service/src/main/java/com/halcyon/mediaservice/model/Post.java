package com.halcyon.mediaservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "owner_email")
    private String ownerEmail;

    public Post(String title, String content, String ownerEmail) {
        this.title = title;
        this.content = content;
        this.ownerEmail = ownerEmail;
    }
}
