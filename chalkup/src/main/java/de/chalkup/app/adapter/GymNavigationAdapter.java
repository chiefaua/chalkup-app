package de.chalkup.app.adapter;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.inject.Inject;

import de.chalkup.app.persistence.GymManager;

public class GymNavigationAdapter extends BaseAdapter {
    @Inject
    private GymManager gymMgr;
    private int registeredDataSetObservers = 0;
    @Inject
    private LayoutInflater inflaterService;

    private DataSetObserver gymObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetInvalidated();
        }
    };

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);

        registeredDataSetObservers++;
        updateGymDataSetObserverState();
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);

        registeredDataSetObservers--;
        updateGymDataSetObserverState();
    }

    private void updateGymDataSetObserverState() {
        if (registeredDataSetObservers == 1) {
            gymMgr.registerDataSetObserver(gymObserver);
        } else if (registeredDataSetObservers == 0) {
            gymMgr.unregisterDataSetObserver(gymObserver);
        }
    }

    @Override
    public int getCount() {
        return gymMgr.getGyms().size();
    }

    @Override
    public Object getItem(int index) {
        return gymMgr.getGyms().get(index);
    }

    @Override
    public long getItemId(int index) {
        return gymMgr.getGyms().get(index).getId();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = inflaterService.inflate(
                    android.R.layout.simple_spinner_dropdown_item, viewGroup, false);
        }
        TextView tv = (TextView) view;

        tv.setText(gymMgr.getGyms().get(index).getName());
        return tv;
    }
}
