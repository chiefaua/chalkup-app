package de.chalkup.app.adapter;

import android.database.DataSetObserver;
import android.widget.BaseAdapter;

public abstract class EventForwardingBaseAdapter extends BaseAdapter {
    private int registeredDataSetObservers = 0;

    private DataSetObserver forwardingObserver = new DataSetObserver() {
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
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);

        registeredDataSetObservers++;
        updateForwardingObserverRegistration();
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);

        registeredDataSetObservers--;
        updateForwardingObserverRegistration();
    }

    private void updateForwardingObserverRegistration() {
        if (registeredDataSetObservers == 1) {
            registerObserver(forwardingObserver);
        } else if (registeredDataSetObservers == 0) {
            unregisterObserver(forwardingObserver);
        }
    }

    protected abstract void registerObserver(DataSetObserver observer);

    protected abstract void unregisterObserver(DataSetObserver observer);
}
