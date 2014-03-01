package de.chalkup.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.List;

import de.chalkup.app.adapter.BoulderListAdapter;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderColor;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.BoulderNotFoundException;
import de.chalkup.app.service.GymNotFoundException;
import de.chalkup.app.service.GymService;
import de.chalkup.app.widget.BoulderListEntryView;
import de.chalkup.app.widget.FloorPlanView;
import roboguice.fragment.RoboListFragment;
import roboguice.inject.InjectView;

public class BoulderListFragment extends RoboListFragment {
    public static final String ARG_GYM_ID = "gym_id";
    private static final String TAG = BoulderListFragment.class.getName();
    private static final String STATE_ACTIVATED_BOULDER = "activated_boulder";
    private static final String STATE_ACTIVATED_COLOR = "activated_color";

    @Inject
    private GymService gymService;

    @InjectView(R.id.floorplan)
    private FloorPlanView floorPlanView;

    private int activatedPosition = ListView.INVALID_POSITION;

    private Gym activeGym;
    private BoulderColor filterBoulderColor;

    public BoulderListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boulder_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_GYM_ID)) {
            try {
                activeGym = gymService.getGym(getArguments().getLong(ARG_GYM_ID));

                if (savedInstanceState != null &&
                        savedInstanceState.containsKey(STATE_ACTIVATED_COLOR)) {
                    String colorName = savedInstanceState.getString(STATE_ACTIVATED_COLOR);
                    for (BoulderColor boulderColor : activeGym.getColors()) {
                        if (boulderColor.getName().equals(colorName)) {
                            filterBoulderColor = boulderColor;
                        }
                    }
                }

                updateDisplayedBoulder();

                if (savedInstanceState != null &&
                        savedInstanceState.containsKey(STATE_ACTIVATED_BOULDER)) {
                    setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_BOULDER));
                }


            } catch (GymNotFoundException e) {
                Log.e(TAG, "Failed to find active gym", e);
            }
        }
    }

    private void updateDisplayedBoulder() {
        floorPlanView.setFloorPlan(activeGym.getFloorPlan());
        setListAdapter(new BoulderListAdapter(getActivity(), activeGym, filterBoulderColor));
    }

    @Override
    public void onDestroyView() {
        for (BoulderListEntryView blev : findBoulderListEntryViews(getListView())) {
            gymService.unregisterBoulderSyncListener(blev);
        }

        super.onDestroyView();
    }

    private List<BoulderListEntryView> findBoulderListEntryViews(ViewGroup group) {
        List<BoulderListEntryView> ret = Lists.newArrayList();

        for (int i = 0; i < group.getChildCount(); ++i) {
            View child = group.getChildAt(i);
            if (child instanceof BoulderListEntryView) {
                ret.add((BoulderListEntryView) child);
            } else if (child instanceof ViewGroup) {
                ret.addAll(findBoulderListEntryViews(group));
            }
        }

        return ret;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        activatedPosition = position;

        try {
            Boulder boulder = activeGym.getBoulder(id);

            if (getActivity() instanceof BoulderListActivity) {
                ((BoulderListActivity) getActivity()).onBoulderSelected(boulder);
            }

            floorPlanView.setBoulders(Collections.singletonList(boulder));
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
        if (filterBoulderColor != null) {
            outState.putString(STATE_ACTIVATED_COLOR, filterBoulderColor.getName());
        }
    }

    public Gym getActiveGym() {
        return activeGym;
    }

    public void setSelectedBoulder(Boulder boulder) {
        ListAdapter adapter = getListView().getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItemId(i) == boulder.getId()) {
                getListView().setItemChecked(i, true);
                activatedPosition = i;
                break;
            }
        }
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(activatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
            Boulder boulder = (Boulder) getListView().getItemAtPosition(position);
            floorPlanView.setBoulders(Collections.singletonList(boulder));
        }

        activatedPosition = position;
    }

    public void setFilterBoulderColor(BoulderColor color) {
        if (Objects.equal(filterBoulderColor, color)) {
            return;
        }
        filterBoulderColor = color;
        updateDisplayedBoulder();
    }
}
