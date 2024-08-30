package com.halcyon.userservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "email")
    @JsonIgnore
    private String email;

    @Column(name = "username")
    private String username;

    @Column(name = "about")
    private String about;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "is_verified")
    private boolean isVerified;

    @Column(name = "password")
    private String password;

    @Column(name = "is_banned")
    private boolean isBanned;

    @Column(name = "is_using_2fa")
    private boolean isUsing2FA;

    @Column(name = "secret")
    private String secret;

    @Column(name = "auth_provider")
    private String authProvider;

    @OneToMany(mappedBy = "owner")
    @JsonBackReference
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "target")
    @JsonBackReference
    private List<Subscription> subscribers;

    @OneToMany(mappedBy = "target")
    @JsonBackReference
    private List<Strike> strikes;
}

