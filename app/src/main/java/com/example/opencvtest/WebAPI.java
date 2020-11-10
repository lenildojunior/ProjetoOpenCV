package com.example.opencvtest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WebAPI {

    @GET("API/get_dispositivos")
    Call<List<Dispositivo>> getDispositivos();

    @GET("API/get_dispositivos/{id}")
    Call<List<Dispositivo>> getDispositivos(@Path("id") String id_dispositivo);

    @GET("API/get_localizacao/{id}")
    Call<List<Localizacao>> getLocalizacao(@Path("id") String id_localizacao);

    @GET("posts")
    Call<List<Post>> getPosts();

    @POST("posts")
    Call<Post> createPost(@Body Post post);
}
