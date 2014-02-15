package de.chalkup.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.EntityNotFoundException;
import de.chalkup.app.service.GymService;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class BoulderDetailFragment extends RoboFragment implements View.OnClickListener {
    public static final String ARG_GYM_ID = "gym_id";
    public static final String ARG_BOULDER_ID = "boulder_id";
    private static final String TAG = BoulderDetailFragment.class.getName();

    @Inject
    private GymService gymMgr;

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

            Uri photoUri = boulder.getPhotoUri(getActivity().getApplicationContext());
            if (new File(photoUri.getPath()).exists()) {
                imageView.setImageURI(photoUri);
            } else {
                imageView.setImageResource(R.drawable.ic_launcher);
            }
        }
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

        startActivityForResult(Intent.createChooser(intent, "Complete action using"),
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
                // force image reload by resetting to the initial value first
                Uri photoUri = boulder.getPhotoUri(getActivity().getApplicationContext());
                imageView.setImageResource(R.drawable.ic_launcher);
                try {
                    FileUtils.copyFile(new File(croppedImageUri.getPath()),
                            new File(photoUri.getPath()));
                } catch (IOException e) {
                    Log.e(TAG, "Failed to copy cropped image", e);
                    Toast.makeText(getActivity(), "Failed to copy cropped image",
                            Toast.LENGTH_SHORT).show();
                }
                imageView.setImageURI(photoUri);

                deleteFile(imageCaptureUri);
                deleteFile(croppedImageUri);
                break;
        }
    }

    private void doCrop() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent, 0);

        int size = list.size();

        if (size == 0) {
            Toast.makeText(getActivity(), "Can not find image crop app", Toast.LENGTH_SHORT).show();
            return;
        } else {
            intent.setData(imageCaptureUri);

            croppedImageUri = Uri.fromFile(
                    new File(getActivity().getApplicationContext().getExternalCacheDir(),
                            "temp.jpg"));

            intent.putExtra("crop", "true");
            intent.putExtra("noFaceDetection", true);
            intent.putExtra("scale", true);
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
