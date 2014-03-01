package de.chalkup.app.widget;

import android.app.Application;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.inject.Inject;

import de.chalkup.app.BoulderListActivity;
import de.chalkup.app.R;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.service.BoulderSyncListener;
import de.chalkup.app.service.GymService;
import roboguice.RoboGuice;

public class BoulderListEntryView extends LinearLayout implements View.OnClickListener, BoulderSyncListener {
    @Inject
    private Application application;

    @Inject
    private GymService gymService;

    private BoulderListActivity boulderListActivity;

    private Boulder boulder;

    private TextView mainText;
    private TextView smallText;
    private ImageButton showPhotoButton;
    private View loadingIndicator;

    public BoulderListEntryView(Context context) {
        super(context);
        initializeView(context);
    }

    public BoulderListEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView(context);
    }

    public BoulderListEntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeView(context);
    }

    private void initializeView(Context context) {
        if (isInEditMode()) {
            return;
        }

        RoboGuice.injectMembers(context, this);
        boulderListActivity = (BoulderListActivity) context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mainText = (TextView) findViewById(R.id.main_text);
        smallText = (TextView) findViewById(R.id.small_text);
        showPhotoButton = (ImageButton) findViewById(R.id.show_photo);
        loadingIndicator = findViewById(R.id.loading_indicator);

        showPhotoButton.setOnClickListener(this);
        findViewById(R.id.image_from_camera).setOnClickListener(this);
        findViewById(R.id.image_from_gallery).setOnClickListener(this);
    }

    public Boulder getBoulder() {
        return boulder;
    }

    public void setBoulder(Boulder boulder) {
        this.boulder = boulder;

        mainText.setText(boulder.getColor().getGermanName() + " (" + boulder.getId() + ")");
        smallText.setText(boulder.getGrade().toFontScale());

        if (boulder.hasCachedPhoto(application) || boulder.hasPhoto()) {
            showPhotoButton.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            showPhotoButton.setImageResource(android.R.drawable.btn_star_big_off);
        }

        gymService.registerBoulderSyncListener(this);
        updateLoadingIndicator();
    }

    @Override
    public void onClick(View view) {
        boulderListActivity.onBoulderSelected(boulder);

        switch (view.getId()) {
            case R.id.show_photo:
                boulderListActivity.showPhoto(getBoulder());
                break;
            case R.id.image_from_camera:
                boulderListActivity.grabImageFromCamera(getBoulder());
                break;
            case R.id.image_from_gallery:
                boulderListActivity.grabImageFromGallery(getBoulder());
                break;
        }
    }

    @Override
    public boolean hasFocusable() {
        // we have to return false here, otherwise this item could not be clicked.
        return false;
    }

    @Override
    public void boulderSyncStarted(Boulder boulder) {
        updateLoadingIndicator();
    }

    @Override
    public void boulderSynced(Boulder boulder) {
        updateLoadingIndicator();
    }

    @Override
    public void boulderSyncFailed(Boulder boulder) {
        updateLoadingIndicator();
    }

    private void updateLoadingIndicator() {
        if (gymService.isSyncingBoulder(boulder)) {
            loadingIndicator.setVisibility(VISIBLE);
            showPhotoButton.setVisibility(INVISIBLE);
        } else {
            showPhotoButton.setVisibility(VISIBLE);
            loadingIndicator.setVisibility(INVISIBLE);
        }
    }
}
