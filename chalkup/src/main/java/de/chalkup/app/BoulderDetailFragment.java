package de.chalkup.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.persistence.EntityNotFoundException;
import de.chalkup.app.persistence.GymManager;

public class BoulderDetailFragment extends Fragment implements View.OnClickListener {
    public static final String ARG_GYM_ID = "gym_id";
    public static final String ARG_BOULDER_ID = "boulder_id";
    private static final String TAG = BoulderDetailFragment.class.getName();
    private Boulder boulder;

    private Uri imageCaptureUri;

    private ImageView imageView;

    public BoulderDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_GYM_ID) &&
                getArguments().containsKey(ARG_BOULDER_ID)) {
            try {
                Gym gym = GymManager.getInstance().getGym(getArguments().getLong(ARG_GYM_ID));
                boulder = gym.getBoulder(getArguments().getLong(ARG_BOULDER_ID));
            } catch (EntityNotFoundException e) {
                Log.e(TAG, "Couldn't find gym or boulder", e);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_boulder_detail, container, false);

        imageView = (ImageView) rootView.findViewById(R.id.boulder_image);

        rootView.findViewById(R.id.image_from_camera).setOnClickListener(this);
        rootView.findViewById(R.id.image_from_gallery).setOnClickListener(this);

        if (boulder != null) {
            getActivity().getActionBar().setTitle(boulder.getName());

            FileInputStream fis = null;
            try {
                fis = getActivity().getApplicationContext().openFileInput(boulder.getPhotoFilename());
                Bitmap photo = BitmapFactory.decodeStream(fis);
                if (photo != null) {
                    imageView.setImageBitmap(photo);
                }
            } catch (FileNotFoundException ignored) {
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignored) {
                    }
                }
            }

        }

        return rootView;
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

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureUri);
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
                Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");

                    imageView.setImageBitmap(photo);

                    FileOutputStream fos = null;
                    try {
                        fos = getActivity().getApplicationContext().openFileOutput(
                                boulder.getPhotoFilename(), Context.MODE_PRIVATE);
                        photo.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }

                File f = new File(imageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
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

            intent.putExtra("crop", "true");
            intent.putExtra("outputY", 96);
            intent.putExtra("noFaceDetection", true);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            ResolveInfo res = list.get(0);
            intent.setComponent(new ComponentName(
                    res.activityInfo.packageName, res.activityInfo.name));

            startActivityForResult(intent, RequestCode.CROP_IMAGE.ordinal());
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
