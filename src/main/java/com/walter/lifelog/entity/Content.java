package com.walter.lifelog.entity;

import com.walter.lifelog.entity.code.ContentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
    name = "contents",
    indexes = {
        @Index(name = "idx_content_type", columnList = "contentType")
    }
)
public record Content(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_seq")
    Long contentSeq,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    ContentType contentType,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "JSON", nullable = false)
    Map<String, Object> content,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt,

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt
) {
}
