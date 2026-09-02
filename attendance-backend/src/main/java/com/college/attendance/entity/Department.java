package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}