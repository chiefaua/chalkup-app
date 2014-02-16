package de.chalkup.app;

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
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.EntityNotFoundException;
import de.chalkup.app.service.GymService;
import de.chalkup.app.service.SyncBoulderAsyncTask;
import de.chalkup.app.service.SyncBoulderCallback;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class BoulderDetailFragment extends RoboFragment implements View.OnClickListener,
        SyncBoulderCallback {
    public static final String ARG_GYM_ID = "gym_id";
    public static final String ARG_BOULDER_ID = "boulder_id";

    private static final int MAX_PHOTO_WIDTH = 500;
    private static final int MAX_PHOTO_HEIGHT = 500;

    private static final String TAG = BoulderDetailFragment.class.getName();

    @Inject
    private GymService gymMgr;

    @InjectView(R.id.loading_panel)
    private View loadingPanel;
    @InjectView(R.id.boulder_image)
    private ImageView imageView;

    private Boulder boulder;
    private Uri imageCaptureUri;
    private Uri croppedImageUri;

    public BoulderDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_GYM_ID) &&
                getArguments().containsKey(ARG_BOULDER_ID)) {
            try {
                Gym gym = gymMgr.getGym(getArguments().getLong(ARG_GYM_ID));
                boulder = gym.getBoulder(getArguments().getLong(ARG_BOULDER_ID));
            } catch (EntityNotFoundException e) {
                Log.e(TAG, "Couldn't find gym or boulder", e);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boulder_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.image_from_camera).setOnClickListener(this);
        view.findViewById(R.id.image_from_gallery).setOnClickListener(this);

        if (boulder != null) {
            getActivity().getActionBar().setTitle(boulder.getName());
            imageView.setImageResource(R.drawable.ic_launcher);
            new SyncBoulderAsyncTask(getActivity(), this).execute(boulder);
        }
    }

    @Override
    public void boulderSyncStarted() {
        loadingPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void boulderSynced(Boulder boulder) {
        if (boulder.hasCachedPhoto(getActivity())) {
            imageView.setImageURI(Uri.fromFile(boulder.getCachePhotoFile(getActivity())));
        }
        loadingPanel.setVisibility(View.GONE);
    }

    @Override
    public void boulderSyncFailed() {
        loadingPanel.setVisibility(View.GONE);
        Toast.makeText(getActivity(), R.string.sync_boulder_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image_from_camera:
                grabImageFromCamera(view);
                break;
            case R.id.image_from_gallery:
                grabImageFromGallery(view);
                break;
        }
    }

    public void grabImageFromCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        imageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageCaptureUri);
        intent.putExtra("return-data", true);

        startActivityForResult(intent, RequestCode.GRAB_FROM_CAMERA.ordinal());
    }

    public void grabImageFromGallery(View view) {
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(
                Intent.createChooser(intent, getActivity().getString(R.string.complete_with)),
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
                imageCaptureUri = data.getData();
                doCrop();
                break;
            case CROP_IMAGE:
                final File cachedPhotoFile =
                        boulder.getCachePhotoFile(getActivity().getApplicationContext());

                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        loadingPanel.setVisibility(View.VISIBLE);
                    }

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        // force image reload by resetting to the initial value first
                        imageView.setImageResource(R.drawable.ic_launcher);
                        try {
                            FileUtils.copyFile(new File(croppedImageUri.getPath()), cachedPhotoFile);

                            scalePhotoToMaximumSize(cachedPhotoFile);
                            return Boolean.TRUE;
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to copy cropped image", e);
                            return Boolean.FALSE;
                        } finally {
                            deleteFile(imageCaptureUri);
                            deleteFile(croppedImageUri);
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success.booleanValue()) {
                            imageView.setImageURI(Uri.fromFile(cachedPhotoFile));
                            new SyncBoulderAsyncTask(getActivity(), BoulderDetailFragment.this)
                                    .execute(boulder);
                        } else {
                            loadingPanel.setVisibility(View.GONE);
                            Toast.makeText(getActivity(), R.string.copy_cropped_image_failed,
                                    Toast.LENGTH_SHORT).show();
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

        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent, 0);

        int size = list.size();

        if (size == 0) {
            Toast.makeText(getActivity(), R.string.crop_app_not_found, Toast.LENGTH_SHORT).show();
            return;
        } else {
            intent.setData(imageCaptureUri);

            croppedImageUri = Uri.fromFile(
                    new File(getActivity().getApplicationContext().getExternalCacheDir(),
                            "temp.jpg"));

            intent.putExtra("crop", "true");
            intent.putExtra("noFaceDetection", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, croppedImageUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name());

            ResolveInfo res = list.get(0);
            intent.setComponent(new ComponentName(
                    res.activityInfo.packageName, res.activityInfo.name));

            startActivityForResult(intent, RequestCode.CROP_IMAGE.ordinal());
        }
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
