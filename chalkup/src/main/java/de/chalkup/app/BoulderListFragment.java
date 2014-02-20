package de.chalkup.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.inject.Inject;

import de.chalkup.app.adapter.BoulderListAdapter;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.FloorPlan;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.BoulderNotFoundException;
import de.chalkup.app.service.GymService;
import de.chalkup.app.service.GymNotFoundException;
import de.chalkup.app.widget.FloorPlanView;
import roboguice.fragment.RoboListFragment;
import roboguice.inject.InjectView;

public class BoulderListFragment extends RoboListFragment {
    public static final String ARG_GYM_ID = "gym_id";
    private static final String TAG = BoulderListFragment.class.getName();
    private static final String STATE_ACTIVATED_BOULDER = "activated_boulder";

    private static Callback dummyCallback = new Callback() {
        @Override
        public void onBoulderSelected(Boulder boulder) {
        }
    };

    private Callback callback = dummyCallback;

    @Inject
    private GymService gymService;

    @InjectView(R.id.floorplan)
    private FloorPlanView floorPlanView;

    private int activatedPosition = ListView.INVALID_POSITION;

    private Gym activeGym;

    public BoulderListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boulder_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_GYM_ID)) {
            try {
                activeGym = gymService.getGym(getArguments().getLong(ARG_GYM_ID));

                setListAdapter(new BoulderListAdapter(getActivity(), activeGym));

                floorPlanView.setFloorPlan(activeGym.getFloorPlan());
                for (Boulder boulder : activeGym.getBoulders()) {
                    floorPlanView.addBoulder(boulder);
                }

                if (savedInstanceState != null
                        && savedInstanceState.containsKey(STATE_ACTIVATED_BOULDER)) {
                    setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_BOULDER));
                }
            } catch (GymNotFoundException e) {
                Log.e(TAG, "Failed to find active gym", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callback)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        callback = (Callback) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = dummyCallback;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        try {
            Boulder boulder = activeGym.getBoulder(id);
            callback.onBoulderSelected(boulder);
        } catch (BoulderNotFoundException e) {
            Log.e(TAG, "Failed to get active boulder", e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activatedPosition != ListView.INVALID_POSITION) {
            outState.putInt(STATE_ACTIVATED_BOULDER, activatedPosition);
        }
    }

    public void setActivateOnItemClick(boolean activateOnItemClick) {
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(activatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        activatedPosition = position;
    }

    public interface Callback {
        public void onBoulderSelected(Boulder boulder);
    }
}
