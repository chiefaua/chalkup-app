package de.chalkup.app.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.List;

import de.chalkup.app.R;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderColor;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.GymService;
import de.chalkup.app.widget.BoulderListEntryView;
import roboguice.RoboGuice;

public class BoulderListAdapter extends EventForwardingBaseAdapter {
    private final Gym gym;
    private final BoulderColor filterBoulderColor;

    private List<Boulder> boulders;

    @Inject
    private GymService gymService;
    @Inject
    private LayoutInflater inflaterService;

    public BoulderListAdapter(Context context, Gym gym, BoulderColor filterBoulderColor) {
        this.gym = gym;
        this.filterBoulderColor = filterBoulderColor;

        RoboGuice.getInjector(context).injectMembers(this);

        boulders = getFilteredBoulders();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    protected void registerObserver(DataSetObserver observer) {
        gymService.registerGymObserver(gym, observer);
    }

    @Override
    protected void unregisterObserver(DataSetObserver observer) {
        gymService.unregisterGymObserver(gym, observer);
    }

    @Override
    public int getCount() {
        return boulders.size();
    }

    @Override
    public Object getItem(int index) {
        return boulders.get(index);
    }

    @Override
    public long getItemId(int index) {
        return boulders.get(index).getId();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = inflaterService.inflate(
                    R.layout.boulder_list_item, viewGroup, false);
        }

        Boulder boulder = boulders.get(index);
        BoulderListEntryView blev = (BoulderListEntryView) view;
        blev.setBoulder(boulder);

        return view;
    }

    @Override
    protected void onChanged() {
        boulders = getFilteredBoulders();
    }

    @Override
    protected void onInvalidated() {
        boulders = getFilteredBoulders();
    }

    private List<Boulder> getFilteredBoulders() {
        return Lists.newArrayList(Iterables.filter(gym.getBoulders(), new Predicate<Boulder>() {
            @Override
            public boolean apply(Boulder input) {
                if (filterBoulderColor == null) {
                    return true;
                }
                return input.getColor().equals(filterBoulderColor);
            }
        }));
    }
}
