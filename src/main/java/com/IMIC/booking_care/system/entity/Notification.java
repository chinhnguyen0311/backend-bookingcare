package com.IMIC.booking_care.system.entity;

import com.IMIC.booking_care.system.enums.NotificationType;
import com.IMIC.booking_care.system.enums.RelatedType;
import com.IMIC.booking_care.user.entity.User;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;


import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "notification_id", columnDefinition = "uuid")
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private NotificationType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_id", columnDefinition = "uuid")
    private UUID relatedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 50)
    private RelatedType relatedType;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "is_sent")
    private Boolean isSent;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
        if (isSent == null) {
            isSent = false;
        }
    }

    // Method để đánh dấu đã đọc
    public void markAsRead() {
        this.isRead = true;
        this.readAt = OffsetDateTime.now();
    }

    // Method để đánh dấu đã gửi
    public void markAsSent() {
        this.isSent = true;
    }
}