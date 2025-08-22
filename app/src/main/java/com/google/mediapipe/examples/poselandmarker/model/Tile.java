package com.google.mediapipe.examples.poselandmarker.model;

import android.graphics.drawable.Drawable;

public class Tile {
    private Type type;
    private Drawable texture;

    public enum Type {
        PAREDE, CHAO, AGUA, GRAMA, EDIFICIO, RUA
    }

    public Tile(Type type, Drawable texture) {
        this.type = type;
        this.texture = texture;
    }

    public Type getType() { return type; }
    public Drawable getTexture() { return texture; }
}