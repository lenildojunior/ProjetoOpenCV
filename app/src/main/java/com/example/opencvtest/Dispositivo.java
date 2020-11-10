package com.example.opencvtest;

import java.text.DateFormat;
import java.util.Date;

public class Dispositivo {
    private String id;
    private String marca_modelo;
    private String data_cadastro;
    private String criado_por;
    private int ativo;

    public String getId() {
        return id;
    }

    public String getMarcaModelo() {
        return marca_modelo;
    }

    public String getDataCadasro() {
        return data_cadastro;
    }

    public String getCriadoPor() {
        return criado_por;
    }

    public int getAtivo() {
        return ativo;
    }
}
