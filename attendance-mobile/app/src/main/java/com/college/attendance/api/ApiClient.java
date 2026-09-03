package com.college.attendance.api;

import android.content.Context;

import com.college.attendance.utils.SessionManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

//    private static final String BASE_URL = "http://10.0.2.2:8080/"; //local
    private static final String BASE_URL = "http://172.16.36.94:8080/"; //noida


    private static Retrofit retrofit;

    public static Retrofit getRetrofit(Context context) {

        if (retrofit == null) {

            SessionManager sessionManager =
                    new SessionManager(
                            context.getApplicationContext()
                    );

            // Logging
            HttpLoggingInterceptor loggingInterceptor =
                    new HttpLoggingInterceptor();

            loggingInterceptor.setLevel(
                    HttpLoggingInterceptor.Level.BODY
            );

            // JWT Authentication Interceptor
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

            // OkHttp Client
            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
                            .addInterceptor(loggingInterceptor)
                            .build();

            // Retrofit
            retrofit = new Retrofit.Builder()
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