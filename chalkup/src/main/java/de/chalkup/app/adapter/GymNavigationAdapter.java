package de.chalkup.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.chalkup.app.model.Gym;
import de.chalkup.app.persistence.GymManager;

public class GymNavigationAdapter extends BaseAdapter {
    private GymManager gymMgr = GymManager.getInstance();
    private int registeredDataSetObservers = 0;
    private Context context;
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

    public GymNavigationAdapter(Context context) {
        this.context = context;
        inflaterService = (LayoutInflater) this.context.getSystemService(
                Activity.LAYOUT_INFLATER_SERVICE);
    }

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
    public View getView(int index, View view, ViewGroup viewGroup) {
        TextView tv = (TextView) inflaterService.inflate(
                android.R.layout.simple_spinner_dropdown_item, null);
        tv.setText(gymMgr.getGyms().get(index).getName());
        return tv;
    }
}
