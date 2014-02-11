package de.chalkup.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.chalkup.app.model.Gym;

public class GymNavigationAdapter extends BaseAdapter {
    private List<Gym> gyms;
    private Context context;
    private View view;
    private LayoutInflater inflaterService;

    public GymNavigationAdapter(Context context, List<Gym> gyms) {
        this.gyms = gyms;
        this.context = context;
        inflaterService = (LayoutInflater) context.getSystemService(
                Activity.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return gyms.size();
    }

    @Override
    public Object getItem(int index) {
        return gyms.get(index);
    }

    @Override
    public long getItemId(int index) {
        return gyms.get(index).getId();
    }

    @Override
    public View getView(int index, View view, ViewGroup viewGroup) {
        TextView tv = (TextView) inflaterService.inflate(
                android.R.layout.simple_spinner_dropdown_item, null);
        tv.setText(gyms.get(index).getName());
        return tv;
    }

    @Override
    public View getDropDownView(int index, View view, ViewGroup viewGroup) {
        return getView(index, view, viewGroup);
    }
}
