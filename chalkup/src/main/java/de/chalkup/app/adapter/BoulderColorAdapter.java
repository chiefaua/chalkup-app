package de.chalkup.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.google.inject.Inject;

import java.util.List;

import de.chalkup.app.R;
import de.chalkup.app.model.BoulderColor;
import roboguice.RoboGuice;

public class BoulderColorAdapter extends BaseAdapter {
    private final List<BoulderColor> colors;

    @Inject
    private LayoutInflater inflaterService;

    public BoulderColorAdapter(Context context, final List<BoulderColor> colors) {
        this.colors = colors;

        RoboGuice.getInjector(context).injectMembers(this);
    }

    @Override
    public int getCount() {
        return colors.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return null;
        }
        return colors.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = inflaterService.inflate(R.layout.boulder_color_item, viewGroup, false);
        }

        ImageView iv = (ImageView) view;
        BoulderColorDrawable drawable = new BoulderColorDrawable((BoulderColor) getItem(position));
        iv.setImageDrawable(drawable);

        return iv;
    }
}
