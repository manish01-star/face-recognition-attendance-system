package com.college.attendance.api;

import com.college.attendance.dto.AttendanceMarkResponse;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.dto.LoginRequest;
import com.college.attendance.dto.LoginResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    // ============================================================
    // LOGIN
    // ============================================================

    @POST("api/auth/login")
    Call<LoginResponse> login(
            @Body LoginRequest request
    );


    // ============================================================
    // CHECK IN
    // ============================================================

    @Multipart
    @POST("api/attendance/check-in")
    Call<AttendanceMarkResponse> checkIn(

            @Part MultipartBody.Part file,

            @Part("latitude")
            RequestBody latitude,

            @Part("longitude")
            RequestBody longitude
    );


    // ============================================================
    // CHECK OUT
    // ============================================================

    @Multipart
    @POST("api/attendance/check-out")
    Call<AttendanceMarkResponse> checkOut(

            @Part MultipartBody.Part file,

            @Part("latitude")
            RequestBody latitude,

            @Part("longitude")
            RequestBody longitude
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
}