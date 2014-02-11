package de.chalkup.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class BoulderDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boulder_detail);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putLong(BoulderDetailFragment.ARG_GYM_ID,
                    getIntent().getLongExtra(BoulderDetailFragment.ARG_GYM_ID, -1));
            arguments.putLong(BoulderDetailFragment.ARG_BOULDER_ID,
                    getIntent().getLongExtra(BoulderDetailFragment.ARG_BOULDER_ID, -1));
            BoulderDetailFragment fragment = new BoulderDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.boulder_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, SessionListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
