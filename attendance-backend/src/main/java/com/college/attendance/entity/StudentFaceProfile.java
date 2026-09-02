package com.college.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_face_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_face_student",
                        columnNames = "student_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFaceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "student_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_student_face_student"
            )
    )
    private Student student;

    @Lob
    @Column(
            name = "embedding",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String embedding;

    @Column(
            name = "embedding_size",
            nullable = false
    )
    private Integer embeddingSize;

    @Column(
            nullable = false,
            length = 50
    )
    private String model;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}