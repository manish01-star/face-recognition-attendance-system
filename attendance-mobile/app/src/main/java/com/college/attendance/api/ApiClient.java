package com.college.attendance.api;

import android.content.Context;

import com.college.attendance.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.164.252:8080/"; // samsung
//    private static final String BASE_URL = "http://172.16.36.94:8080/"; // noida
//    private static final String BASE_URL = "http://10.183.83.252:8080/"; // realme

    private static Retrofit retrofit;

    public static Retrofit getRetrofit(Context context) {

        if (retrofit == null) {

            SessionManager sessionManager =
                    new SessionManager(
                            context.getApplicationContext()
                    );

            // =========================
            // LOGGING
            // =========================
            HttpLoggingInterceptor loggingInterceptor =
                    new HttpLoggingInterceptor();

            loggingInterceptor.setLevel(
                    HttpLoggingInterceptor.Level.BASIC
            );

            // =========================
            // JWT AUTHENTICATION
            // =========================
            Interceptor authInterceptor = chain -> {

                Request originalRequest = chain.request();

                String token =
                        sessionManager.getAccessToken();

                Request.Builder requestBuilder =
                        originalRequest.newBuilder();

                // Add JWT only when token exists
                if (token != null &&
                        !token.trim().isEmpty()) {

                    requestBuilder.addHeader(
                            "Authorization",
                            "Bearer " + token
                    );
                }

                return chain.proceed(
                        requestBuilder.build()
                );
            };

            // =========================
            // OKHTTP CLIENT
            // =========================
            OkHttpClient client =
                    new OkHttpClient.Builder()

                            // Server se connection establish
                            // hone ka maximum time
                            .connectTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            )

                            // Server response wait time
                            .readTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )

                            // Image upload ka maximum time
                            .writeTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )

                            .addInterceptor(authInterceptor)
                            .addInterceptor(loggingInterceptor)
                            .build();

            // =========================
            // RETROFIT
            // =========================
            retrofit =
                    new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();
        }

        return retrofit;
    }

    public static ApiService getApiService(
            Context context
    ) {

        return getRetrofit(context)
                .create(ApiService.class);
    }
}