package com.example.todolist.entities;

public class Categorias {
    private int id;
    private String nombre;
    private String userId;

    public Categorias() {
    }

    public Categorias(String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Categorias(int id, String nombre, String userId) {
        this.id = id;
        this.nombre = nombre;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
