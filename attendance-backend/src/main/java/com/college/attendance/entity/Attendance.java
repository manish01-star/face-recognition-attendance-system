package com.college.attendance.entity;

import com.college.attendance.entity.enums.AttendanceSource;
import com.college.attendance.entity.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_user_date",
                        columnNames = {
                                "user_id",
                                "attendance_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_attendance_date",
                        columnList = "attendance_date"
                ),
                @Index(
                        name = "idx_attendance_user",
                        columnList = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_attendance_user"
            )
    )
    private User user;

    @Column(
            name = "attendance_date",
            nullable = false
    )
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    /*
     * ============================================================
     * MOBILE LOCATION
     * ============================================================
     */

    @Column
    private Double checkInLatitude;

    @Column
    private Double checkInLongitude;

    @Column
    private Double checkOutLatitude;

    @Column
    private Double checkOutLongitude;

    /*
     * ============================================================
     * ATTENDANCE SOURCE
     * ============================================================
     *
     * MOBILE  -> Attendance marked from mobile app
     * MACHINE -> Attendance marked from college machine
     */

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source",
            nullable = false,
            length = 20
    )
    private AttendanceSource source;

    /*
     * ============================================================
     * ATTENDANCE STATUS
     * ============================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private AttendanceStatus status;

    /*
     * Face recognition confidence/similarity score.
     */

    private Double confidence;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}