package de.chalkup.app.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.inject.Inject;

import de.chalkup.app.R;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.GymService;
import de.chalkup.app.widget.BoulderListEntryView;
import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;

public class BoulderListAdapter extends EventForwardingBaseAdapter {
    @Inject
    private GymService gymService;
    @Inject
    private LayoutInflater inflaterService;

    private Gym gym;

    public BoulderListAdapter(Context context, Gym gym) {
        this.gym = gym;

        RoboGuice.getInjector(context).injectMembers(this);
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
        return gym.getBoulders().size();
    }

    @Override
    public Object getItem(int index) {
        return gym.getBoulders().get(index);
    }

    @Override
    public long getItemId(int index) {
        return gym.getBoulders().get(index).getId();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = inflaterService.inflate(
                    R.layout.boulder_list_item, viewGroup, false);
        }

        Boulder boulder = gym.getBoulders().get(index);
        BoulderListEntryView blev = (BoulderListEntryView) view;
        blev.setBoulder(boulder);

        return view;
    }
}
