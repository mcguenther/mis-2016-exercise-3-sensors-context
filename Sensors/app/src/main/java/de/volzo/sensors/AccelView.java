package de.volzo.sensors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

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
            } catch (ClassCastException ce) {
                return;
            }
        }

        // draw graph here.
        int height = canvas.getHeight();
        int width = canvas.getWidth();
        int elements = main.x.size();

        if (elements == 0) {
            System.out.println(elements);
            return;
        }

        int step = width/elements;

        Double[] x = new Double[elements];
        Double[] y = new Double[elements];
        Double[] z = new Double[elements];
        Double[] m = new Double[elements];
        main.x.toArray(x);
        main.y.toArray(y);
        main.z.toArray(z);
        main.m.toArray(m);

        float lastX = 0;
        float lastY = 0;

        for (int i = 0; i < x.length; i++) {
            float vX = i * step;
            float vY = (float) (double) x[i];

            canvas.drawLine(lastX, lastY, i*step, (float) (double) x[i], paint);

            lastX = vX;
            lastY = vY;
        }

    }

    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(200, 200);
    }





}
