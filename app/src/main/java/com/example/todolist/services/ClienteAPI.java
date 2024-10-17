package com.example.todolist.services;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClienteAPI {
    private static final String BASE_URL = "https://66ec62a02b6cf2b89c5e47b6.mockapi.io/";
    private static ServicioAPI instancia;

    public static ServicioAPI obtenerInstancia() {
        if (instancia == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            instancia = retrofit.create(ServicioAPI.class);
        }
        return instancia;
    }
}