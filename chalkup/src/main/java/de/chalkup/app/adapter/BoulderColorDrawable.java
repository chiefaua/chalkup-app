package de.chalkup.app.adapter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.google.common.collect.Lists;

import java.util.List;

import de.chalkup.app.model.BoulderColor;
import de.chalkup.app.widget.DrawHelper;

public class BoulderColorDrawable extends Drawable {
    private final BoulderColor boulderColor;

    public BoulderColorDrawable(BoulderColor boulderColor) {
        this.boulderColor = boulderColor;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        PointF center = new PointF(bounds.exactCenterX(), bounds.exactCenterY());

        List<Integer> colors;
        if (boulderColor != null) {
            colors = boulderColor.getColors();
        } else {
            colors = Lists.newArrayList();
            for (int s = 0; s < 2; s++) {
                for (int h = 0; h < 5; h++) {
                    float saturation = 0.5f + s / 2.0f;
                    float hue = 360.0f * h / 5.0f;
                    colors.add(Color.HSVToColor(255, new float[]{hue, saturation, 1.0f}));
                }
            }
        }

        DrawHelper.drawColorCircle(colors, canvas, center,
                Math.min(bounds.height(), bounds.width()) / 2.0f);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
