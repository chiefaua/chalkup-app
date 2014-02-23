package de.chalkup.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.chalkup.app.adapter.GymNavigationAdapter;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.GymNotFoundException;
import de.chalkup.app.service.GymService;
import de.chalkup.app.service.GymSyncCallback;
import de.chalkup.app.service.SyncBoulderAsyncTask;
import de.chalkup.app.service.SyncBoulderCallback;
import de.chalkup.app.widget.BoulderListEntryView;
import roboguice.activity.RoboFragmentActivity;

public class BoulderListActivity extends RoboFragmentActivity
        implements BoulderListFragment.Callback, ActionBar.OnNavigationListener, GymSyncCallback {
    private static final String TAG = BoulderListActivity.class.getName();

    private static final int MAX_PHOTO_WIDTH = 800;
    private static final int MAX_PHOTO_HEIGHT = 800;

    @Inject
    private GymService gymService;
    @Inject
    private GymNavigationAdapter gymNavigationAdapter;

    private boolean refreshingGyms = false;
    private Menu optionsMenu;

    private BoulderListEntryView currentBoulderListEntryView;
    private Uri currentImageCaptureUri;
    private Uri currentCroppedImageUri;

    /**
     * Whether or not the activity is in two-pane mode.
     */
    private boolean mTwoPane;

    private Gym activeGym;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boulder_list);

        if (findViewById(R.id.boulder_detail_container) != null) {
            mTwoPane = true;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(gymNavigationAdapter, this);

        gymService.syncGyms(this, GymService.SyncMode.FAST_FROM_CACHE, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.boulder_list_actions, menu);
        updateRefreshButton();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_boulder && activeGym != null) {
            Boulder boulder = new Boulder(activeGym);
            gymService.addBoulderToGym(activeGym, boulder);
            onBoulderSelected(boulder);
            return true;
        } else if (item.getItemId() == R.id.refresh_boulder) {
            gymService.syncGyms(this, GymService.SyncMode.ALLOW_CACHE, this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void syncStarted() {
        refreshingGyms = true;
        updateRefreshButton();
    }

    @Override
    public void syncFinished() {
        refreshingGyms = false;
        updateRefreshButton();
    }

    private void updateRefreshButton() {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu
                    .findItem(R.id.refresh_boulder);
            if (refreshingGyms) {
                refreshItem.setActionView(R.layout.actionbar_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    @Override
    public void onBoulderSelected(Boulder boulder) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putLong(BoulderDetailFragment.ARG_GYM_ID, boulder.getGym().getId());
            arguments.putLong(BoulderDetailFragment.ARG_BOULDER_ID, boulder.getId());
            BoulderDetailFragment fragment = new BoulderDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.boulder_detail_container, fragment)
                    .commit();
        } else {
            Intent detailIntent = new Intent(this, BoulderDetailActivity.class);
            detailIntent.putExtra(BoulderDetailFragment.ARG_GYM_ID, boulder.getGym().getId());
            detailIntent.putExtra(BoulderDetailFragment.ARG_BOULDER_ID, boulder.getId());
            startActivity(detailIntent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        try {
            activeGym = gymService.getGym(itemId);

            BoulderListFragment listFragment = new BoulderListFragment();
            Bundle args = new Bundle();
            args.putLong(BoulderListFragment.ARG_GYM_ID, activeGym.getId());
            listFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.boulder_list_container, listFragment,
                            "GYM_LIST_" + activeGym.getId())
                    .commit();

            return true;
        } catch (GymNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void grabImageFromCamera(BoulderListEntryView boulderListEntryView) {
        this.currentBoulderListEntryView = boulderListEntryView;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        currentImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageCaptureUri);
        intent.putExtra("return-data", true);

        startActivityForResult(intent, RequestCode.GRAB_FROM_CAMERA.ordinal());
    }

    public void grabImageFromGallery(BoulderListEntryView boulderListEntryView) {
        this.currentBoulderListEntryView = boulderListEntryView;
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(
                Intent.createChooser(intent, getString(R.string.complete_with)),
                RequestCode.GRAB_FROM_GALLERY.ordinal());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        RequestCode gs = RequestCode.fromOrdinal(requestCode);
        switch (gs) {
            case GRAB_FROM_CAMERA:
                doCrop();
                break;
            case GRAB_FROM_GALLERY:
                currentImageCaptureUri = data.getData();
                doCrop();
                break;
            case CROP_IMAGE:
                Boulder boulder = currentBoulderListEntryView.getBoulder();
                final File cachedPhotoFile =
                        boulder.getCachePhotoFile(this);

                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        currentBoulderListEntryView.showLoading();
                    }

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = getContentResolver().openInputStream(currentCroppedImageUri);
                            os = FileUtils.openOutputStream(cachedPhotoFile);
                            IOUtils.copy(is, os);

                            scalePhotoToMaximumSize(cachedPhotoFile);
                            return Boolean.TRUE;
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to copy cropped image", e);
                            return Boolean.FALSE;
                        } finally {
                            IOUtils.closeQuietly(is);
                            IOUtils.closeQuietly(os);

                            revokeUriPermission(currentCroppedImageUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            deleteFile(currentImageCaptureUri);
                            deleteFile(currentCroppedImageUri);
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success.booleanValue()) {
                            syncCurrentBoulder();
                        } else {
                            currentBoulderListEntryView.hideLoading();
                            Toast.makeText(BoulderListActivity.this,
                                    R.string.copy_cropped_image_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute();

                break;
        }
    }

    private void scalePhotoToMaximumSize(File cachedPhotoFile) throws FileNotFoundException {
        Bitmap bitmap = BitmapFactory.decodeFile(cachedPhotoFile.getPath());

        if (bitmap == null) {
            return;
        }

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        if (height <= MAX_PHOTO_HEIGHT && width <= MAX_PHOTO_WIDTH) {
            return;
        }

        float scale = Math.min((float) MAX_PHOTO_HEIGHT / (float) height,
                (float) MAX_PHOTO_WIDTH / (float) width);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        bitmap.recycle();

        OutputStream os = null;
        try {
            os = new FileOutputStream(cachedPhotoFile);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private void doCrop() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

        int size = list.size();

        if (size == 0) {
            Toast.makeText(this, R.string.crop_app_not_found, Toast.LENGTH_SHORT).show();
            return;
        } else {
            ResolveInfo res = list.get(0);


            File croppedImagesPath = new File(getApplicationContext().getCacheDir(),
                    "cropped_images");
            croppedImagesPath.mkdirs();
            File croppedImage = new File(croppedImagesPath, "temp.jpg");
            currentCroppedImageUri = FileProvider.getUriForFile(this,
                    "de.chalkup.app.fileprovider", croppedImage);
            grantUriPermission(res.activityInfo.packageName, currentCroppedImageUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.setData(currentImageCaptureUri);
            intent.putExtra("crop", "true");
            intent.putExtra("noFaceDetection", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentCroppedImageUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name());

            intent.setComponent(new ComponentName(
                    res.activityInfo.packageName, res.activityInfo.name));

            startActivityForResult(intent, RequestCode.CROP_IMAGE.ordinal());
        }
    }

    private void syncCurrentBoulder() {
        final Boulder boulder = currentBoulderListEntryView.getBoulder();
        new SyncBoulderAsyncTask(this,
                new SyncBoulderCallback() {
                    @Override
                    public void boulderSyncStarted() {
                        currentBoulderListEntryView.showLoading();
                    }

                    @Override
                    public void boulderSynced(Boulder boulder) {
                        boulderSyncDone();
                    }

                    @Override
                    public void boulderSyncFailed() {
                        boulderSyncDone();
                        Toast.makeText(BoulderListActivity.this, R.string.sync_boulder_failed,
                                Toast.LENGTH_SHORT).show();
                    }

                    private void boulderSyncDone() {
                        gymService.boulderChanged(boulder);
                        currentBoulderListEntryView.hideLoading();
                    }
                }).execute(boulder);
    }

    private void deleteFile(Uri uri) {
        File f = new File(uri.getPath());
        if (f.exists()) {
            f.delete();
        }
    }

    private enum RequestCode {
        GRAB_FROM_CAMERA, GRAB_FROM_GALLERY, CROP_IMAGE;

        public static RequestCode fromOrdinal(int ordinal) {
            for (RequestCode gs : RequestCode.values()) {
                if (gs.ordinal() == ordinal) {
                    return gs;
                }
            }

            throw new IllegalArgumentException("Invalid ordinal " + ordinal);
        }
    }
}
