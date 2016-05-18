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
        int elements = main.x.size();

        if (elements == 0) {
            return;
        }

        int fractionHeight = height / 4;

        drawLine(canvas, 0, width, fractionHeight * 0, fractionHeight * 1, main.x);
        drawLine(canvas, 0, width, fractionHeight * 1, fractionHeight * 2, main.y);
        drawLine(canvas, 0, width, fractionHeight * 2, fractionHeight * 3, main.z);
        drawLine(canvas, 0, width, fractionHeight * 3, fractionHeight * 4, main.m);

    }

    private void drawLine(Canvas canvas, int x1, int x2, int y1, int y2, CircularFifoQueue<Double> values) {
        Double[] v = new Double[values.size()];
        values.toArray(v);

        float min = (float) (double) Collections.min(values);
        float max = (float) (double) Collections.max(values);

        int offset = (y2 - y1) / 2;

        float lastX = x1;
        float lastY = y1 + offset;

        int step = (x2 - x1) / v.length;

        for (int i = 0; i < v.length; i++) {
            float vX = i * step;
            float vY = (float) (double) v[i];

            // normalize
            vY = (vY - min) / (max - min);
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
