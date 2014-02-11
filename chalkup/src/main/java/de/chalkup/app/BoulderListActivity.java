package de.chalkup.app;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

import de.chalkup.app.adapter.GymNavigationAdapter;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.Gym;
import de.chalkup.app.persistence.GymManager;
import de.chalkup.app.persistence.GymNotFoundException;

public class BoulderListActivity extends FragmentActivity
        implements BoulderListFragment.Callback, ActionBar.OnNavigationListener {

    private static final String TAG = BoulderListActivity.class.getName();

    /**
     * Whether or not the activity is in two-pane mode.
     */
    private boolean mTwoPane;

    private Gym activeGym;

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

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        GymNavigationAdapter adapter = new GymNavigationAdapter(getApplicationContext());
        actionBar.setListNavigationCallbacks(adapter, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.boulder_list_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_boulder) {
            Boulder boulder = new Boulder(activeGym, "new boulder");
            activeGym.addBoulder(boulder);
            onBoulderSelected(boulder);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBoulderSelected(Boulder boulder) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putLong(BoulderDetailFragment.ARG_GYM_ID, boulder.getGym().getId());
            arguments.putLong(BoulderDetailFragment.ARG_BOULDER_ID, boulder.getId());
            BoulderDetailFragment fragment = new BoulderDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.boulder_detail_container, fragment)
                    .commit();
        } else {
            Intent detailIntent = new Intent(this, BoulderDetailActivity.class);
            detailIntent.putExtra(BoulderDetailFragment.ARG_GYM_ID, boulder.getGym().getId());
            detailIntent.putExtra(BoulderDetailFragment.ARG_BOULDER_ID, boulder.getId());
            startActivity(detailIntent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        try {
            activeGym = GymManager.getInstance().getGym(itemId);

            BoulderListFragment listFragment = new BoulderListFragment();
            Bundle args = new Bundle();
            args.putLong(BoulderListFragment.ARG_GYM_ID, activeGym.getId());
            listFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.boulder_list_container, listFragment,
                            "GYM_LIST_" + activeGym.getId())
                    .commit();

            return true;
        } catch (GymNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
