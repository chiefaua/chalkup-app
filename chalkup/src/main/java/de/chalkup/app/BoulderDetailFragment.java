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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;

public class BoulderDetailFragment extends Fragment implements View.OnClickListener {
    public static final String ARG_ITEM_ID = "item_id";
    private Boulder mBoulder;

    private Uri mImageCaptureUri;

    private ImageView mImageView;

    public BoulderDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mBoulder = Gym.BOULDER_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_boulder_detail, container, false);

        mImageView = (ImageView) rootView.findViewById(R.id.boulder_image);

        rootView.findViewById(R.id.image_from_camera).setOnClickListener(this);
        rootView.findViewById(R.id.image_from_gallery).setOnClickListener(this);

        if (mBoulder != null) {
            ((TextView) rootView.findViewById(R.id.boulder_detail)).setText(mBoulder.content);
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

        mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
        intent.putExtra("return-data", true);

        startActivityForResult(intent, RequestCode.GRAB_FROM_CAMERA.ordinal());
    }

    public void grabImageFromGallery(View view) {
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

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
                mImageCaptureUri = data.getData();
                doCrop();
                break;
            case CROP_IMAGE:
                Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");

                    mImageView.setImageBitmap(photo);
                }
                File f = new File(mImageCaptureUri.getPath());

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
            intent.setData(mImageCaptureUri);

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
