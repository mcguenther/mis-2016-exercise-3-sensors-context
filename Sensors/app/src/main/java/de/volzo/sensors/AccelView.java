package de.volzo.sensors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Johannes on 11.05.2016.
 */
public class AccelView extends View {

    private Context context;

    private MainActivity main;
    private Paint paint = new Paint();

    private float global_min = 0;
    private float global_max = 0;

    public AccelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
    }



    @Override
    protected void onDraw (Canvas canvas) {

        if (main == null) {
            try {
                main = (MainActivity) context;
                paint.setColor(Color.RED);
            } catch (Exception ce) {
                return;
            }
        }

        int height = canvas.getHeight();
        int width = canvas.getWidth();
        int elements = main.ax.length;

        if (elements == 0) {
            return;
        }

        if (main.ax[0] == null) {
            return;
        }

        int fractionHeight = height / 4;

        global_min = (float) (double) Collections.min(main.x);
        if (global_min > Collections.min(main.y)) { global_min = (float) (double) Collections.min(main.y); }
        if (global_min > Collections.min(main.z)) { global_min = (float) (double) Collections.min(main.z); }
        if (global_min > Collections.min(main.m)) { global_min = (float) (double) Collections.min(main.m); }

        global_max = (float) (double) Collections.max(main.x);
        if (global_max < Collections.max(main.y)) { global_max = (float) (double) Collections.max(main.y); }
        if (global_max < Collections.max(main.z)) { global_max = (float) (double) Collections.max(main.z); }
        if (global_max < Collections.max(main.m)) { global_max = (float) (double) Collections.max(main.m); }

        drawLine(canvas, 0, width, fractionHeight * 0, fractionHeight * 1, main.ax);
        drawLine(canvas, 0, width, fractionHeight * 1, fractionHeight * 2, main.ay);
        drawLine(canvas, 0, width, fractionHeight * 2, fractionHeight * 3, main.az);
        drawLine(canvas, 0, width, fractionHeight * 3, fractionHeight * 4, main.am);

    }

    private void drawLine(Canvas canvas, int x1, int x2, int y1, int y2, Double[] v) {
        int offset = (y2 - y1) / 2;

        float lastX = x1;
        float lastY = y1 + offset;

        int step = (x2 - x1) / v.length;

        for (int i = 0; i < v.length; i++) {
            if (v[i] == null) {continue;}

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

    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(200, 200);
    }





}
