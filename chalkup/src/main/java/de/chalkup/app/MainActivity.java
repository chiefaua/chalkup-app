package de.chalkup.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import roboguice.activity.RoboActivity;

public class MainActivity extends RoboActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void manageBoulder(View view) {
        Intent intent = new Intent(this, BoulderListActivity.class);
        startActivity(intent);
    }

    public void addSession(View view) {
        Intent intent = new Intent(this, SessionListActivity.class);
        startActivity(intent);
    }
}
