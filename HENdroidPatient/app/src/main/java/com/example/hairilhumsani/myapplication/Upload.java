package com.example.hairilhumsani.myapplication;

public class Upload {
    private String imageUrl;

    public Upload() {
        //empty constructor
    }

    public Upload(String name, String imageUrl) {
        imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imgUrl) {
        imageUrl = imgUrl;
    }

}