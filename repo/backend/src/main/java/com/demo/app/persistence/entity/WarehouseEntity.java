package com.demo.app.persistence.entity;

import com.demo.app.domain.model.Warehouse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Warehouse toModel() {
        return Warehouse.builder()
                .id(id)
                .name(name)
                .code(code)
                .location(location)
                .active(active)
                .build();
    }
}
