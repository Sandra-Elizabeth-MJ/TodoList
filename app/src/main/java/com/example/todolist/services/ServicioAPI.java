package com.example.todolist.services;

import com.example.todolist.entities.Categorias;
import com.example.todolist.entities.Tarea;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ServicioAPI {
        @GET("Tarea")
        Call<List<Tarea>> obtenerTareas();

        @POST("Tarea")
        Call<Tarea> crearTarea(@Body Tarea tarea);

        @GET("Tarea/{id}")
        Call<Tarea> obtenerTarea(@Path("id") int id);

        @PUT("Tarea/{id}")
        Call<Tarea> actualizarTarea(@Path("id") int id, @Body Tarea tarea);

        @GET("Categorias")
        Call<List<Categorias>> obtenerCategorias();

        @POST("Categorias")
        Call<Categorias> crearCategoria(@Body Categorias categorias);

}

