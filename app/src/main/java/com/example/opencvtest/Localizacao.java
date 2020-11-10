package com.example.opencvtest;

import java.text.DateFormat;

public class Localizacao {
    private String id_dispositivo;
    private String latitude;
    private String longitude;
    private int qtd_faixas;
    private DateFormat data_realocacao;

    public String getId_dispositivo() {
        return id_dispositivo;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public int getQtdFaixas() {
        return qtd_faixas;
    }

    public DateFormat getDataRealocacao() {
        return data_realocacao;
    }
}
