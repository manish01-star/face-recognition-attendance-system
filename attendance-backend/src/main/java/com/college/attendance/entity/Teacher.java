package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "teachers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_teacher_employee_code",
                        columnNames = "employee_code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

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
                    name = "fk_teacher_user"
            )
    )
    private User user;

    @Column(
            name = "employee_code",
            nullable = false,
            length = 50
    )
    private String employeeCode;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department_id",
            foreignKey = @ForeignKey(
                    name = "fk_teacher_department"
            )
    )
    private Department department;
}