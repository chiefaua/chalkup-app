package de.chalkup.app;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.inject.Inject;

import java.util.List;

import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.BoulderSyncMode;
import de.chalkup.app.service.EntityNotFoundException;
import de.chalkup.app.service.GymService;
import de.chalkup.app.service.SyncBoulderAsyncTask;
import de.chalkup.app.service.SyncBoulderCallback;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class BoulderDetailFragment extends RoboFragment implements SyncBoulderCallback {
    public static final String ARG_GYM_ID = "gym_id";
    public static final String ARG_BOULDER_ID = "boulder_id";

    private static final String TAG = BoulderDetailFragment.class.getName();

    @Inject
    private GymService gymMgr;

    @InjectView(R.id.loading_panel)
    private View loadingPanel;
    @InjectView(R.id.boulder_image)
    private ImageView imageView;

    private Boulder boulder;

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

        if (boulder != null) {
            imageView.setImageResource(R.drawable.ic_launcher);
            new SyncBoulderAsyncTask(getActivity(), BoulderSyncMode.FULL_SYNC, this)
                    .execute(boulder);
        }
    }

    @Override
    public void boulderSyncStarted() {
        loadingPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void boulderSynced(List<Boulder> boulders) {
        updateImageView();
        loadingPanel.setVisibility(View.GONE);
    }

    @Override
    public void boulderSyncFailed() {
        updateImageView();
        loadingPanel.setVisibility(View.GONE);
        Toast.makeText(getActivity(), R.string.sync_boulder_failed, Toast.LENGTH_SHORT).show();
    }

    private void updateImageView() {
        if (boulder.hasCachedPhoto(getActivity())) {
            imageView.setImageURI(Uri.fromFile(boulder.getCachePhotoFile(getActivity())));
        }
    }
}
