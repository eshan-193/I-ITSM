package com.itsm.domain.incident;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A message on an incident thread. Owned by its incident (composition). */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 4000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommentType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Comment() {
        // for JPA
    }

    Comment(Incident incident, User author, String text, CommentType type, LocalDateTime createdAt) {
        this.incident = incident;
        this.author = author;
        this.text = text;
        this.type = type;
        this.createdAt = createdAt;
    }

    public boolean isVisibleTo(User viewer) {
        return type.isVisibleToReporter() || viewer.getRole() != Role.EMPLOYEE;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public User getAuthor() { return author; }
    public String getText() { return text; }
    public CommentType getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
