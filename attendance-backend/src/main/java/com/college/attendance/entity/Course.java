package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_department"))
    private Department department;

    @Column
    private Integer durationYears;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}