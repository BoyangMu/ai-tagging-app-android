package com.example.comp433finalproject;

import android.graphics.Bitmap;

public class TaggedImage {
    private Bitmap image;
    private String tags;
    private String timestamp;
    private Boolean isSketch;

    public TaggedImage(Bitmap image, String tags, String timestamp, Boolean isSketch) {
        this.image = image;
        this.tags = tags;
        this.timestamp = timestamp;
        this.isSketch = isSketch;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTags() {
        return tags;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Boolean getIsSketch() { return isSketch; }
}