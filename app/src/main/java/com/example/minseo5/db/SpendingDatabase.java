package com.example.minseo5.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SpendingRecord.class}, version = 2)
public abstract class SpendingDatabase extends RoomDatabase {

    private static volatile SpendingDatabase instance;

    public abstract SpendingDao spendingDao();

    public static SpendingDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SpendingDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SpendingDatabase.class,
                            "spending.db"
                    ).allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
