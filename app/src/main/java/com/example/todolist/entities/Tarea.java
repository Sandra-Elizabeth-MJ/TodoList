package com.example.todolist.entities;

import java.io.Serializable;

public class Tarea implements Serializable {
    public String id;
    public String nombre;
    public String fecha;
    public String hora;
    public String categoria;
    public String userId;

    public Tarea() {
    }

    public Tarea(String nombre, String fecha, String hora, String categoria) {
        this.nombre = nombre;
        this.fecha = fecha;
        this.hora = hora;
        this.categoria = categoria;
    }
    public Tarea(String nombre, String id,String fecha, String hora, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.hora = hora;
        this.categoria = categoria;
    }

    public Tarea(String id, String nombre, String fecha, String hora, String categoria, String userId) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.hora = hora;
        this.categoria = categoria;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
