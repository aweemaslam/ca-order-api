package com.caorderapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "order_statuses")
@Getter
@Setter
@RequiredArgsConstructor
public class OrderStatusEntity extends BaseEntity implements Serializable {

    @Id
    @Column(name = "status_code", nullable = false, length = 50)
    private String statusCode;

    @Column(nullable = false)
    private String description;

}

