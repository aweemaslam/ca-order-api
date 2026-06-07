package com.caorderapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "update_status_restriction")
@Getter
@Setter
public class UpdateStatusRestrictionEntity extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "update_status_restriction_id", nullable = false)
    private Integer id;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus;

    @Column(name = "allowed_next_status", nullable = false, length = 50)
    private String allowedNextStatus;
}

