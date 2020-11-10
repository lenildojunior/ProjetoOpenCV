package com.example.opencvtest;

import com.google.gson.annotations.SerializedName;

public class Post {
    private int userId;
    private Integer id;
    private String titlte;

    public Post(int userId, String titlte, String text) {
        this.userId = userId;
        this.titlte = titlte;
        this.text = text;
    }

    @SerializedName("body")
    private String text;

    public int getUserId() {
        return userId;
    }

    public int getId() {
        return id;
    }

    public String getTitlte() {
        return titlte;
    }

    public String getText() {
        return text;
    }
}
