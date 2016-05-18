package de.volzo.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Johannes on 11.05.2016.
 */
public class AccelView extends View {

    public AccelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw (Canvas canvas) {
        // draw graph here.
        Canvas.draw()
    }

    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(200, 200);
    }





}
