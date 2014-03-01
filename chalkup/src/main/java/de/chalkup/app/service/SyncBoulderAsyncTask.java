package de.chalkup.app.service;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import de.chalkup.app.model.Boulder;
import roboguice.RoboGuice;

class SyncBoulderAsyncTask extends AsyncTask<Boulder, Void, List<Boulder>> {
    private static final String TAG = SyncBoulderAsyncTask.class.getName();

    @Inject
    private Application application;
    @Inject
    private BackendService backendService;

    private BoulderSyncMode boulderSyncMode;
    private BoulderSyncCallback callback;

    public SyncBoulderAsyncTask(Context context, BoulderSyncMode boulderSyncMode,
                                BoulderSyncCallback callback) {
        this.boulderSyncMode = boulderSyncMode;
        this.callback = callback;
        RoboGuice.getInjector(context).injectMembers(this);
    }

    @Override
    protected void onPreExecute() {
        callback.boulderSyncStarted();
    }

    @Override
    protected List<Boulder> doInBackground(Boulder... boulders) {
        for (Boulder boulder : boulders) {
            try {
                syncBoulder(boulder);
            } catch (IOException e) {
                Log.e(TAG, "Failed to sync boulder", e);
                return null;
            }
        }
        return Arrays.asList(boulders);
    }

    @Override
    protected void onPostExecute(List<Boulder> boulders) {
        if (boulders != null) {
            callback.boulderSynced(boulders);
        } else {
            callback.boulderSyncFailed();
        }
    }

    private void syncBoulder(Boulder boulder) throws IOException {
        File cachePhotoFile = boulder.getCachePhotoFile(application);
        if (boulder.hasCachedPhoto(application) && boulder.hasPhoto()) {
            long photoLastModified = backendService.getFileLastModified(boulder.getPhotoUrl());
            if (photoLastModified > cachePhotoFile.lastModified()) {
                downloadPhoto(boulder);
            } else if (photoLastModified < cachePhotoFile.lastModified()) {
                uploadPhoto(boulder);
            }
        } else if (boulder.hasCachedPhoto(application)) {
            uploadPhoto(boulder);
        } else if (boulder.hasPhoto()) {
            downloadPhoto(boulder);
        }
    }

    private void downloadPhoto(Boulder boulder) throws IOException {
        if (boulderSyncMode != BoulderSyncMode.FULL_SYNC &&
                boulderSyncMode != BoulderSyncMode.DOWNLOAD_ONLY) {
            return;
        }

        File targetFile = boulder.getCachePhotoFile(application);
        backendService.downloadFile(boulder.getPhotoUrl(), targetFile);
    }

    private void uploadPhoto(Boulder boulder) throws IOException {
        if (boulderSyncMode != BoulderSyncMode.FULL_SYNC &&
                boulderSyncMode != BoulderSyncMode.UPLOAD_ONLY) {
            return;
        }

        File sourceFile = boulder.getCachePhotoFile(application);
        URL photoUrl = backendService.uploadFile(sourceFile,
                backendService.getApiUrl("/boulders/" + boulder.getId() + "/photo"),
                "image/jpeg");
        boulder.setPhotoUrl(photoUrl);
    }

    public static interface BoulderSyncCallback {
        void boulderSyncStarted();

        void boulderSynced(List<Boulder> boulder);

        void boulderSyncFailed();
    }
}
