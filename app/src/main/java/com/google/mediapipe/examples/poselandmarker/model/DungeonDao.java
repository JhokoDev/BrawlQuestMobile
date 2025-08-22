package com.google.mediapipe.examples.poselandmarker.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface DungeonDao {
    @Query("SELECT * FROM dungeons WHERE id = :id")
    DungeonEntity getDungeon(String id);

    @Insert
    void insert(DungeonEntity dungeon);
}