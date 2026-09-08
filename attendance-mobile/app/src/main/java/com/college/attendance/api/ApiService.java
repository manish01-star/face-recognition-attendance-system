package com.college.attendance.api;

import com.college.attendance.dto.AttendanceMarkResponse;
import com.college.attendance.dto.AttendancePolicyResponse;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.dto.HolidayResponse;
import com.college.attendance.dto.LeaveApplyRequest;
import com.college.attendance.dto.LeaveResponse;
import com.college.attendance.dto.LoginRequest;
import com.college.attendance.dto.LoginResponse;
import com.college.attendance.dto.UserProfileResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    // ============================================================
    // AUTH
    // ============================================================

    @POST("api/auth/login")
    Call<LoginResponse> login(
            @Body LoginRequest request
    );

    // ============================================================
    // USER PROFILE
    // ============================================================

    @GET("api/users/profile")
    Call<UserProfileResponse> getMyProfile();

    // ============================================================
    // ATTENDANCE - CHECK IN
    // ============================================================

    @Multipart
    @POST("api/attendance/check-in")
    Call<AttendanceMarkResponse> checkIn(
            @Part MultipartBody.Part file,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude
    );


    // ============================================================
    // ATTENDANCE - CHECK OUT
    // ============================================================

    @Multipart
    @POST("api/attendance/check-out")
    Call<AttendanceMarkResponse> checkOut(
            @Part MultipartBody.Part file,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude
    );


    // ============================================================
    // MY ATTENDANCE
    // ============================================================

    @GET("api/attendance/my")
    Call<List<AttendanceResponse>> getMyAttendance();


    // ============================================================
    // MY ATTENDANCE BY DATE
    // ============================================================

    @GET("api/attendance/my/date/{date}")
    Call<AttendanceResponse> getMyAttendanceByDate(
            @Path("date") String date
    );


    // ============================================================
    // ATTENDANCE POLICY
    // ============================================================

    @GET("api/attendance-policies/current")
    Call<AttendancePolicyResponse> getCurrentPolicy();


    // ============================================================
    // LEAVE - USER
    // ============================================================

    // POST /api/leaves
    @POST("api/leaves")
    Call<LeaveResponse> applyLeave(
            @Body LeaveApplyRequest request
    );


    // GET /api/leaves/my
    @GET("api/leaves/my")
    Call<List<LeaveResponse>> getMyLeaves();


    // GET /api/leaves/my/upcoming
    @GET("api/leaves/my/upcoming")
    Call<List<LeaveResponse>> getMyUpcomingLeaves();


    // GET /api/leaves/{id}
    @GET("api/leaves/{id}")
    Call<LeaveResponse> getLeaveById(
            @Path("id") Long id
    );


    // ============================================================
    // HOLIDAY - USER
    // ============================================================

    // GET /api/holidays
    @GET("api/holidays")
    Call<List<HolidayResponse>> getHolidays();


    // GET /api/holidays/year/{year}
    @GET("api/holidays/year/{year}")
    Call<List<HolidayResponse>> getHolidaysByYear(
            @Path("year") int year
    );


    // GET /api/holidays/{id}
    @GET("api/holidays/{id}")
    Call<HolidayResponse> getHolidayById(
            @Path("id") Long id
    );


    // GET /api/holidays/date/{date}
    @GET("api/holidays/date/{date}")
    Call<HolidayResponse> getHolidayByDate(
            @Path("date") String date
    );


    // ============================================================
    // ADMIN HOLIDAY
    // ============================================================

    // GET /api/holidays/admin
    @GET("api/holidays/admin")
    Call<List<HolidayResponse>> getAdminHolidays();


    // POST /api/holidays
    @POST("api/holidays")
    Call<HolidayResponse> createHoliday(
            @Body HolidayResponse request
    );


    // PUT /api/holidays/{id}
    @PUT("api/holidays/{id}")
    Call<HolidayResponse> updateHoliday(
            @Path("id") Long id,
            @Body HolidayResponse request
    );


    // DELETE /api/holidays/{id}
    @DELETE("api/holidays/{id}")
    Call<Void> deleteHoliday(
            @Path("id") Long id
    );


    // PUT /api/holidays/{id}/activate
    @PUT("api/holidays/{id}/activate")
    Call<HolidayResponse> activateHoliday(
            @Path("id") Long id
    );

}