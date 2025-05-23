package com.example.mobigait.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.mobigait.dao.GaitDao;
import com.example.mobigait.database.AppDatabase;
import com.example.mobigait.model.GaitData;

import java.util.List;

public class GaitRepository {
    private GaitDao gaitDao;
    private LiveData<GaitData> latestGaitData;

    public GaitRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        gaitDao = db.gaitDao();
        latestGaitData = gaitDao.getLatestGaitData();
    }

    public LiveData<GaitData> getLatestGaitData() {
        return latestGaitData;
    }

    public LiveData<List<GaitData>> getGaitDataBetweenDates(long startTime, long endTime) {
        return gaitDao.getGaitDataBetweenDates(startTime, endTime);
    }

    public LiveData<List<GaitData>> getRecentGaitData(int limit) {
        return gaitDao.getRecentGaitData(limit);
    }

    public void insertGaitData(GaitData gaitData) {
        new InsertGaitDataAsyncTask(gaitDao).execute(gaitData);
    }

    private static class InsertGaitDataAsyncTask extends AsyncTask<GaitData, Void, Void> {
        private GaitDao gaitDao;

        InsertGaitDataAsyncTask(GaitDao gaitDao) {
            this.gaitDao = gaitDao;
        }

        @Override
        protected Void doInBackground(GaitData... gaitData) {
            gaitDao.insert(gaitData[0]);
            return null;
        }
    }

    public void deleteAllGaitData() {
        new DeleteAllGaitDataAsyncTask(gaitDao).execute();
    }

    private static class DeleteAllGaitDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private GaitDao gaitDao;

        DeleteAllGaitDataAsyncTask(GaitDao gaitDao) {
            this.gaitDao = gaitDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            gaitDao.deleteAllGaitData();
            return null;
        }
    }
}