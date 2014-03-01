package de.chalkup.app.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.List;

import de.chalkup.app.R;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderLocation;
import de.chalkup.app.model.FloorPlan;
import de.chalkup.app.model.Gym;
import de.chalkup.app.service.GymService;
import roboguice.RoboGuice;

public class FloorPlanView extends TouchImageView {
    @Inject
    private GymService gymService;

    private List<Boulder> boulders = Lists.newArrayList();
    private FloorPlan floorPlan;
    private Gym gym;
    private DataSetObserver gymObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            setGym(gym);
        }

        @Override
        public void onInvalidated() {
            setGym(gym);
        }
    };

    public FloorPlanView(Context context) {
        super(context);
    }

    public FloorPlanView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloorPlanView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void sharedConstructor(Context context) {
        super.sharedConstructor(context);

        setMaxZoom(5.0f);

        if (!isInEditMode()) {
            RoboGuice.getInjector(context).injectMembers(this);
        }
    }

    public void setFloorPlan(FloorPlan floorPlan) {
        this.floorPlan = floorPlan;

        if (floorPlan != null) {
            setImageURI(floorPlan.getUri());
        } else {
            setImageResource(R.drawable.ic_launcher);
        }
    }

    public void setBoulders(List<Boulder> boulders) {
        this.boulders = Lists.newArrayList(boulders);
        invalidate();
    }

    public void setGym(Gym gym) {
        if (this.gym != gym) {
            if (this.gym != null) {
                gymService.unregisterGymObserver(this.gym, gymObserver);
            }
            this.gym = gym;
            if (this.gym != null) {
                gymService.registerGymObserver(this.gym, gymObserver);
            }
        }

        // refresh even if the same gym was set again
        if (gym != null) {
            setFloorPlan(gym.getFloorPlan());
            setBoulders(gym.getBoulders());
        } else {
            setFloorPlan(null);
            setBoulders(Collections.<Boulder>emptyList());
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        setGym(null);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Boulder boulder : boulders) {
            drawBoulder(boulder, canvas);
        }
    }

    private void drawBoulder(Boulder boulder, Canvas canvas) {
        PointF pos = locationToViewPos(boulder.getLocation());
        float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7,
                getResources().getDisplayMetrics());

        DrawHelper.drawColorCircle(boulder.getColor().getColors(), canvas, pos, radius);
    }

    private PointF locationToViewPos(BoulderLocation location) {
        Drawable drawable = getDrawable();
        if (drawable == null || location == null) {
            return new PointF(0, 0);
        }
        float x = (float) location.getX() * drawable.getIntrinsicWidth();
        float y = (float) location.getY() * drawable.getIntrinsicHeight();

        return getViewPointFromDrawablePoint(x, y);
    }
}
