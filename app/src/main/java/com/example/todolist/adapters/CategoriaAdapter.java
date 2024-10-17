package com.example.todolist.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.todolist.R;

import java.util.List;

public class CategoriaAdapter extends ArrayAdapter<String> {

    public CategoriaAdapter(@NonNull Context context, List<String> categorias) {
        super(context, android.R.layout.simple_spinner_item, categorias);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView text = (TextView) view.findViewById(android.R.id.text1);
        if (position == getCount() - 1) {
            text.setText("Crear nueva categoría");
            text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0);
            text.setCompoundDrawablePadding(8);
            text.setTextColor(getContext().getResources().getColor(R.color.azulA1));
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView text = (TextView) view.findViewById(android.R.id.text1);
        if (position == getCount() - 1) {
            text.setText("Crear nueva categoría");
            text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0);
            text.setCompoundDrawablePadding(8);
            // Cambiar el color del texto
            text.setTextColor(getContext().getResources().getColor(R.color.azulA1));
        }
        return view;
    }
}
