package de.chalkup.app.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.google.common.collect.Lists;

import java.util.List;

import de.chalkup.app.R;
import de.chalkup.app.model.Boulder;
import de.chalkup.app.model.BoulderLocation;
import de.chalkup.app.model.FloorPlan;

public class FloorPlanView extends TouchImageView {
    private FloorPlan floorPlan;
    private final List<Boulder> boulders = Lists.newArrayList();

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
    }

    public void setFloorPlan(FloorPlan floorPlan) {
        this.floorPlan = floorPlan;

        if (floorPlan != null) {
            setImageURI(floorPlan.getUri());
        } else {
            setImageResource(R.drawable.ic_launcher);
        }
    }

    public void addBoulder(Boulder boulder) {
        boulders.add(boulder);
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
        Paint paint = new Paint();
        paint.setColor(boulder.getColor());
        canvas.drawCircle(pos.x, pos.y, 25.0f, paint);
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
