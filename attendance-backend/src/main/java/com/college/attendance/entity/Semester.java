package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semesters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_semester_course_number", columnNames = {
                "course_id",
                "semester_number"
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_semester_course"))
    private Course course;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}