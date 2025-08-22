package com.google.mediapipe.examples.poselandmarker.DataNest;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import android.content.Context;
import androidx.room.Room;

import com.google.mediapipe.examples.poselandmarker.model.DungeonDao;
import com.google.mediapipe.examples.poselandmarker.model.DungeonEntity;

@Database(entities = {DungeonEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DungeonDao dungeonDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "dungeon_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}