package com.utn.vinoteca.features.notifications.entity;

import com.utn.vinoteca.features.notifications.enums.NotificationType;
import com.utn.vinoteca.features.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId", nullable = false, unique = true, updatable = false)
    @UuidGenerator
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;

    @Column(name="is_read",nullable = false)
    private Boolean read;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Builder.Default
    @Column(name = "sent", nullable = false)
    private Boolean sent = false;

}
