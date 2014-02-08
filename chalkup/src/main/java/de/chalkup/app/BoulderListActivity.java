package de.chalkup.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class BoulderListActivity extends FragmentActivity
        implements BoulderListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boulder_list);

        if (findViewById(R.id.boulder_detail_container) != null) {
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((BoulderListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.boulder_list))
                    .setActivateOnItemClick(true);
        }
    }

    @Override
    public void onBoulderSelected(String id) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(BoulderDetailFragment.ARG_ITEM_ID, id);
            BoulderDetailFragment fragment = new BoulderDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.boulder_detail_container, fragment)
                    .commit();

        } else {
            Intent detailIntent = new Intent(this, BoulderDetailActivity.class);
            detailIntent.putExtra(BoulderDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
