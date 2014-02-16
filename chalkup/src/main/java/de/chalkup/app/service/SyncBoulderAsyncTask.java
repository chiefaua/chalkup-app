package de.chalkup.app.service;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import de.chalkup.app.model.Boulder;
import roboguice.RoboGuice;

public class SyncBoulderAsyncTask extends AsyncTask<Boulder, Void, Boulder> {
    private static final String TAG = SyncBoulderAsyncTask.class.getName();

    @Inject
    private Application application;
    @Inject
    private BackendService backendService;

    private SyncBoulderCallback callback;

    public SyncBoulderAsyncTask(Context context, SyncBoulderCallback callback) {
        this.callback = callback;
        RoboGuice.getInjector(context).injectMembers(this);
    }

    @Override
    protected void onPreExecute() {
        callback.boulderSyncStarted();
    }

    @Override
    protected Boulder doInBackground(Boulder... params) {
        Boulder boulder = params[0];
        try {
            syncBoulder(boulder);
        } catch (IOException e) {
            Log.e(TAG, "Failed to sync boulder", e);
            return null;
        }
        return boulder;
    }

    @Override
    protected void onPostExecute(Boulder boulder) {
        if (boulder != null) {
            callback.boulderSynced(boulder);
        } else {
            callback.boulderSyncFailed();
        }
    }

    private void syncBoulder(Boulder boulder) throws IOException {
        File cachePhotoFile = boulder.getCachePhotoFile(application);

        if (boulder.hasPhoto()) {
            if (boulder.getLastSyncedTimestamp() == Boulder.NEVER_SYNCED) {
                downloadPhoto(boulder);
                return;
            }

            // TODO: check if remote photo is newer than the cached one
        } else {
            if (boulder.hasCachedPhoto(application)) {
                uploadPhoto(boulder);
                return;
            }
        }

        if (boulder.hasCachedPhoto(application)) {
            if (cachePhotoFile.lastModified() > boulder.getLastSyncedTimestamp()) {
                uploadPhoto(boulder);
                return;
            }
        }
    }

    private void downloadPhoto(Boulder boulder) throws IOException {
        File targetFile = boulder.getCachePhotoFile(application);
        backendService.downloadFile(boulder.getPhotoUrl(), targetFile);
        boulder.setLastSyncedTimestamp(targetFile.lastModified());
    }

    private void uploadPhoto(Boulder boulder) throws IOException {
        File sourceFile = boulder.getCachePhotoFile(application);
        URL photoUrl = backendService.uploadFile(sourceFile,
                "/boulders/" + boulder.getId() + "/photo",
                "image/jpeg");
        boulder.setPhotoUrl(photoUrl);
        boulder.setLastSyncedTimestamp(sourceFile.lastModified());
    }
}
