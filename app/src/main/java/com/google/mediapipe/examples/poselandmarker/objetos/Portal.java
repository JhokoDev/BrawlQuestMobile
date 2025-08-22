package com.google.mediapipe.examples.poselandmarker.objetos;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import com.google.mediapipe.examples.poselandmarker.model.DungeonType;

public class Portal {
    private GeoPoint location;
    private String name;
    private DungeonType type; // Alterado de String para DungeonType
    private String rank; // E, D, C, B, A, S
    private double preferenceScore;
    private int[] animationFrames;
    private Marker marker;
    private long seed; // Seed para geração consistente da dungeon

    public Portal(GeoPoint location, String name, DungeonType type, String rank, double preferenceScore, int[] animationFrames, long seed) {
        this.location = location;
        this.name = name;
        this.type = type;
        this.rank = rank;
        this.preferenceScore = preferenceScore;
        this.animationFrames = animationFrames;
        this.seed = seed;
    }

    public GeoPoint getLocation() { return location; }
    public String getName() { return name; }
    public DungeonType getType() { return type; }
    public String getRank() { return rank; }
    public double getPreferenceScore() { return preferenceScore; }
    public void setPreferenceScore(double score) { this.preferenceScore = score; }
    public int[] getAnimationFrames() { return animationFrames; }
    public void setMarker(Marker marker) { this.marker = marker; }
    public Marker getMarker() { return marker; }
    public long getSeed() { return seed; }
}