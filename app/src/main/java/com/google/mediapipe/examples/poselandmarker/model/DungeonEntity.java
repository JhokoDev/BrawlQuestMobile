package com.google.mediapipe.examples.poselandmarker.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dungeons")
public class DungeonEntity {
    @PrimaryKey
    @NonNull
    public String id; // Baseado em portal name + seed
    public String layoutJson; // Mapa como JSON
    public long generatedDate;
}