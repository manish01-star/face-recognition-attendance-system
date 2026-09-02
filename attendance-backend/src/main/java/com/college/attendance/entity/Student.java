package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "students",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_roll_number",
                        columnNames = "roll_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_student_user"
            )
    )
    private User user;

    @Column(
            name = "roll_number",
            nullable = false,
            length = 50
    )
    private String rollNumber;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_student_course"
            )
    )
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "semester_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_student_semester"
            )
    )
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "section_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_student_section"
            )
    )
    private Section section;
}