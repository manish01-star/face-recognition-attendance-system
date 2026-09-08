package com.college.attendance.dto.leave;

import java.time.LocalDate;

import com.college.attendance.entity.enums.LeaveType;

public class LeaveApplyRequest {

    private LocalDate fromDate;

    private LocalDate toDate;

    private LeaveType leaveType;

    private String description;


    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }


    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }


    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}