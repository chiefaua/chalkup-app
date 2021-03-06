package de.chalkup.app.adapter;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.inject.Inject;

import de.chalkup.app.service.GymService;

public class GymNavigationAdapter extends EventForwardingBaseAdapter {
    @Inject
    private GymService gymService;
    @Inject
    private LayoutInflater inflaterService;

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    protected void registerObserver(DataSetObserver observer) {
        gymService.registerGymsObserver(observer);
    }

    @Override
    protected void unregisterObserver(DataSetObserver observer) {
        gymService.unregisterGymsObserver(observer);
    }

    @Override
    public int getCount() {
        return gymService.getGyms().size();
    }

    @Override
    public Object getItem(int index) {
        return gymService.getGyms().get(index);
    }

    @Override
    public long getItemId(int index) {
        return gymService.getGyms().get(index).getId();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = inflaterService.inflate(
                    android.R.layout.simple_spinner_dropdown_item, viewGroup, false);
        }
        TextView tv = (TextView) view;

        tv.setText(gymService.getGyms().get(index).getName());
        return tv;
    }
}
