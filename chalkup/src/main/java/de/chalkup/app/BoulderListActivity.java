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
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
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

import de.chalkup.app.adapter.BoulderColorAdapter;
import de.chalkup.app.adapter.GymNavigationAdapter;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderColor;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.BoulderSyncListener;
import de.chalkup.app.service.BoulderSyncMode;
import de.chalkup.app.service.EntityNotFoundException;
import de.chalkup.app.service.GymNotFoundException;
import de.chalkup.app.service.GymService;
import de.chalkup.app.service.GymSyncListener;
import de.chalkup.app.service.GymSyncMode;
import roboguice.activity.RoboFragmentActivity;

public class BoulderListActivity extends RoboFragmentActivity
        implements ActionBar.OnNavigationListener, GymSyncListener,
        BoulderSyncListener {
    private static final String TAG = BoulderListActivity.class.getName();

    private static final String STATE_GYM_ID = "gym_id";
    private static final String STATE_BOULDER_ID = "boulder_id";

    private static final String STATE_ACTIVATED_COLOR = "activated_boulder_color";

    private static final int MAX_PHOTO_WIDTH = 800;
    private static final int MAX_PHOTO_HEIGHT = 800;

    @Inject
    private GymService gymService;
    @Inject
    private GymNavigationAdapter gymNavigationAdapter;

    private Menu optionsMenu;
    private BoulderListFragment boulderListFragment;
    private Uri tempImageCaptureUri;

    private Uri tempCroppedImageUri;

    private String selectedBoulderColorName;
    private long currentGymId = Gym.INVALID_ID;
    private long currentBoulderId = Boulder.INVALID_ID;

    /**
     * Whether or not the activity is in two-pane mode.
     */
    private boolean mTwoPane;

    private Gym activeGym;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File capturedImagesPath = new File(getApplicationContext().getCacheDir(),
                "captured_images");
        capturedImagesPath.mkdirs();
        File capturedImage = new File(capturedImagesPath, "temp.jpg");
        tempImageCaptureUri = FileProvider.getUriForFile(this,
                "de.chalkup.app.fileprovider", capturedImage);

        File croppedImagesPath = new File(getApplicationContext().getCacheDir(),
                "cropped_images");
        croppedImagesPath.mkdirs();
        File croppedImage = new File(croppedImagesPath, "temp.jpg");
        tempCroppedImageUri = FileProvider.getUriForFile(this,
                "de.chalkup.app.fileprovider", croppedImage);


        setContentView(R.layout.activity_boulder_list);

        if (findViewById(R.id.boulder_detail_container) != null) {
            mTwoPane = true;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(gymNavigationAdapter, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.boulder_list_actions, menu);
        updateRefreshButton();
        updateColorSpinner();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();

        gymService.registerGymSyncListener(this);
        gymService.registerBoulderSyncListener(this);
    }

    @Override
    protected void onStop() {
        gymService.unregisterGymSyncListener(this);
        gymService.unregisterBoulderSyncListener(this);

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_GYM_ID, currentGymId);
        outState.putLong(STATE_BOULDER_ID, currentBoulderId);
        outState.putString(STATE_ACTIVATED_COLOR, selectedBoulderColorName);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentGymId = savedInstanceState.getLong(STATE_GYM_ID);
        currentBoulderId = savedInstanceState.getLong(STATE_BOULDER_ID);
        selectedBoulderColorName = savedInstanceState.getString(STATE_ACTIVATED_COLOR);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_boulder && activeGym != null) {
            Boulder boulder = new Boulder(activeGym);
            gymService.addBoulderToGym(activeGym, boulder);
            return true;
        } else if (item.getItemId() == R.id.refresh_boulder) {
            gymService.syncGyms(this, GymSyncMode.ALLOW_CACHE, BoulderSyncMode.UPLOAD_ONLY);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void syncStarted() {
        updateRefreshButton();
    }

    @Override
    public void syncFinished() {
        updateRefreshButton();
    }

    private void updateRefreshButton() {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu
                    .findItem(R.id.refresh_boulder);
            if (gymService.isSyncingGyms()) {
                refreshItem.setActionView(R.layout.actionbar_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    private void updateColorSpinner() {
        if (optionsMenu != null && activeGym != null) {
            Spinner colorSpinner = (Spinner) optionsMenu.findItem(R.id.boulder_color).getActionView();

            colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    BoulderColor color = (BoulderColor) parent.getItemAtPosition(position);
                    if (color != null) {
                        selectedBoulderColorName = color.getName();
                    } else {
                        selectedBoulderColorName = null;
                    }
                    boulderListFragment.setFilterBoulderColor(color);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedBoulderColorName = null;
                    boulderListFragment.setFilterBoulderColor(null);
                }
            });

            colorSpinner.setAdapter(new BoulderColorAdapter(this, activeGym.getColors()));
            if (selectedBoulderColorName != null) {
                SpinnerAdapter adapter = colorSpinner.getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    BoulderColor color = (BoulderColor) adapter.getItem(i);
                    if (color != null && color.getName().equals(selectedBoulderColorName)) {
                        colorSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        try {
            activeGym = gymService.getGym(itemId);

            BoulderListFragment oldFragment = (BoulderListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.boulder_list_container);
            if (oldFragment != null && oldFragment.getActiveGym() != null &&
                    activeGym.getId() == oldFragment.getActiveGym().getId()) {
                // keep the existing fragment
                boulderListFragment = oldFragment;
            } else {
                boulderListFragment = new BoulderListFragment();
                Bundle args = new Bundle();
                args.putLong(BoulderListFragment.ARG_GYM_ID, activeGym.getId());
                boulderListFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.boulder_list_container, boulderListFragment,
                                "GYM_LIST_" + activeGym.getId())
                        .commit();
            }

            updateColorSpinner();
            return true;
        } catch (GymNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void onBoulderSelected(Boulder boulder) {
        showPhoto(boulder, false);
        boulderListFragment.setSelectedBoulder(boulder);
    }

    public void showPhoto(Boulder boulder) {
        showPhoto(boulder, true);
    }

    private void showPhoto(Boulder boulder, boolean openNewViewIfNecessary) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putLong(BoulderDetailFragment.ARG_GYM_ID, boulder.getGym().getId());
            arguments.putLong(BoulderDetailFragment.ARG_BOULDER_ID, boulder.getId());
            BoulderDetailFragment fragment = new BoulderDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.boulder_detail_container, fragment)
                    .commit();
        } else if (openNewViewIfNecessary) {
            Intent detailIntent = new Intent(this, BoulderDetailActivity.class);
            detailIntent.putExtra(BoulderDetailFragment.ARG_GYM_ID, boulder.getGym().getId());
            detailIntent.putExtra(BoulderDetailFragment.ARG_BOULDER_ID, boulder.getId());
            startActivity(detailIntent);
        }
    }

    public void grabImageFromCamera(Boulder boulder) {
        setCurrentBoulder(boulder);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageCaptureUri);
        intent.putExtra("return-data", true);

        ComponentName componentName = intent.resolveActivity(getPackageManager());
        grantUriPermission(componentName.getPackageName(), tempImageCaptureUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, RequestCode.GRAB_FROM_CAMERA.ordinal());
    }

    public void grabImageFromGallery(Boulder boulder) {
        setCurrentBoulder(boulder);

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
                doCrop(tempImageCaptureUri);
                break;
            case GRAB_FROM_GALLERY:
                doCrop(data.getData());
                break;
            case CROP_IMAGE:
                final Boulder boulder = getCurrentBoulder();
                if (boulder == null) {
                    return;
                }

                final File cachedPhotoFile =
                        boulder.getCachePhotoFile(this);

                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                    }

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = getContentResolver().openInputStream(tempCroppedImageUri);
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

                            revokeUriPermission(tempImageCaptureUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            revokeUriPermission(tempCroppedImageUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            deleteFile(tempImageCaptureUri);
                            deleteFile(tempCroppedImageUri);
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success.booleanValue()) {
                            syncBoulder(boulder);
                        } else {
                            Toast.makeText(BoulderListActivity.this,
                                    R.string.copy_cropped_image_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute();

                break;
        }

    }

    private Boulder getCurrentBoulder() {
        try {
            Gym gym = gymService.getGym(currentGymId);
            return gym.getBoulder(currentBoulderId);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    private void setCurrentBoulder(Boulder boulder) {
        currentGymId = boulder.getGym().getId();
        currentBoulderId = boulder.getId();
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

    private void doCrop(Uri imageUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

        int size = list.size();

        if (size == 0) {
            Toast.makeText(this, R.string.crop_app_not_found, Toast.LENGTH_SHORT).show();
            return;
        } else {
            ResolveInfo res = list.get(0);

            grantUriPermission(res.activityInfo.packageName, imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            grantUriPermission(res.activityInfo.packageName, tempCroppedImageUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.setData(imageUri);
            intent.putExtra("crop", "true");
            intent.putExtra("noFaceDetection", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempCroppedImageUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name());

            intent.setComponent(new ComponentName(
                    res.activityInfo.packageName, res.activityInfo.name));

            startActivityForResult(intent, RequestCode.CROP_IMAGE.ordinal());
        }
    }

    private void syncBoulder(final Boulder boulder) {
        gymService.syncBoulder(this, boulder, BoulderSyncMode.FULL_SYNC);
    }

    @Override
    public void boulderSyncStarted(Boulder boulder) {
    }

    @Override
    public void boulderSynced(Boulder boulder) {
        boulderSyncDone(boulder);
    }

    @Override
    public void boulderSyncFailed(Boulder boulder) {
        boulderSyncDone(boulder);
        Toast.makeText(BoulderListActivity.this, R.string.sync_boulder_failed,
                Toast.LENGTH_SHORT).show();
    }

    private void boulderSyncDone(Boulder boulder) {
        gymService.boulderChanged(boulder);
    }

    private void deleteFile(Uri uri) {
        if (uri == null) {
            return;
        }
        File f = new File(uri.getPath());
        f.delete();
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
