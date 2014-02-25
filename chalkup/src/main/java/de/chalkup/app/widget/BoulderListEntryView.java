package de.chalkup.app.widget;

import android.app.Application;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.inject.Inject;

import de.chalkup.app.BoulderListActivity;
import de.chalkup.app.R;
import de.chalkup.app.model.Boulder;
import roboguice.RoboGuice;

public class BoulderListEntryView extends LinearLayout implements View.OnClickListener {
    @Inject
    private Application application;

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

    public void showLoading() {
        loadingIndicator.setVisibility(VISIBLE);
        showPhotoButton.setVisibility(INVISIBLE);
    }

    public void hideLoading() {
        showPhotoButton.setVisibility(VISIBLE);
        loadingIndicator.setVisibility(INVISIBLE);
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
    }

    public Boulder getBoulder() {
        return boulder;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.show_photo:
                boulderListActivity.showPhoto(getBoulder());
                break;
            case R.id.image_from_camera:
                boulderListActivity.grabImageFromCamera(this);
                break;
            case R.id.image_from_gallery:
                boulderListActivity.grabImageFromGallery(this);
                break;
        }
    }

    @Override
    public boolean hasFocusable() {
        // we have to return false here, otherwise this item could not be clicked.
        return false;
    }
}
