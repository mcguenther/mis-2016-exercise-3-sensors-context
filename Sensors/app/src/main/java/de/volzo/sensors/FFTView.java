package de.volzo.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Johannes on 11.05.2016.
 */
public class FFTView extends View {

    private Context context;

    private Double[] magnitudes = new Double[0];
    private Paint paint = new Paint();

    private float global_min = 0;
    private float global_max = 0;

    public FFTView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public void setMagnitudes(Double[] newMagnitudes) {
        this.magnitudes = newMagnitudes;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int elements = magnitudes.length;
        if (elements > 0) {
            int height = canvas.getHeight();
            int width = canvas.getWidth();

            int fractionHeight = height;

            global_min = (float) (double) Collections.min(Arrays.asList(magnitudes));
            global_max = (float) (double) Collections.max(Arrays.asList(magnitudes));

            drawLine(canvas, 0, width, 0, fractionHeight, magnitudes);
        }
    }


    private void drawLine(Canvas canvas, int x1, int x2, int y1, int y2, Double[] v) {
        int offset = (y2 - y1) / 2;

        float lastX = x1;
        float lastY = y1 + offset;

        int step = (x2 - x1) / v.length;

        for (int i = 0; i < v.length; i++) {
            if (v[i] == null) {
                continue;
            }

            float vX = i * step;
            float vY = (float) (double) v[i];

            // normalize
            vY = (vY - global_min) / (global_max - global_min);
            vY = vY * (y2 - y1) + y1;

            canvas.drawLine(lastX, lastY, vX, vY, paint);

            lastX = vX;
            lastY = vY;
        }

    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(200, 200);
    }


}
