package com.example.mobigait.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mobigait.dao.GaitDao;
import com.example.mobigait.model.GaitData;
import com.example.mobigait.model.Step;
import com.example.mobigait.model.Weight;

@Database(entities = {Step.class, Weight.class, GaitData.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "mobigait_db";
    private static AppDatabase instance;

    public abstract StepDao stepDao();
    public abstract WeightDao weightDao();
    public abstract GaitDao gaitDao();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create the gait_data table with the correct structure
            database.execSQL("CREATE TABLE IF NOT EXISTS `gait_data` (" +
                    "`timestamp` INTEGER NOT NULL PRIMARY KEY, " +
                    "`status` TEXT NOT NULL, " +
                    "`cadence` REAL NOT NULL, " +
                    "`step_variability` REAL NOT NULL, " +
                    "`symmetry_index` REAL NOT NULL, " +
                    "`step_length` REAL NOT NULL)");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            .addMigrations(MIGRATION_2_3)
                            .fallbackToDestructiveMigration() // In case migration fails
                            .build();
                }
            }
        }
        return instance;
    }
}
