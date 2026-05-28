package com.caorderapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Base entity class to provide automatic creation and update timestamps.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * Timestamp when the entity was created (automatically set, not updatable).
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    protected Instant createdAt;

    /**
     * Timestamp when the entity was last updated (automatically set).
     */
    @UpdateTimestamp
    protected Instant updatedAt;

    @Column(name = "is_active", nullable = false)
    protected boolean active;

}