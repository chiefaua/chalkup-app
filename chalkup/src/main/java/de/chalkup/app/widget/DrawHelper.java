package de.chalkup.app.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.List;

public class DrawHelper {
    public static void drawColorCircle(List<Integer> colors, Canvas canvas, PointF center, float radiusPx) {
        RectF rect = new RectF(center.x - radiusPx, center.y - radiusPx,
                center.x + radiusPx, center.y + radiusPx);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        float startAngle = 225.0f;
        float sweepAngle = 360.0f / colors.size();

        for (Integer color : colors) {
            paint.setColor(color);

            canvas.drawArc(rect, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }
    }
}
