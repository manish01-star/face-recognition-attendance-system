package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sections", uniqueConstraints = {
        @UniqueConstraint(name = "uk_section_semester_name", columnNames = {
                "semester_id",
                "name"
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_section_semester"))
    private Semester semester;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}