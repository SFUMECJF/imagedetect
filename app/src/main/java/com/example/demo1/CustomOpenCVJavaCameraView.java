package com.example.demo1;
import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
public class CustomOpenCVJavaCameraView extends JavaCameraView{
    public CustomOpenCVJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private Paint linePaint;
    protected void init() {
        Resources r = this.getResources();
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setAlpha(200);
        linePaint.setStrokeWidth(1);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(r.getColor(R.color.black));
        linePaint.setShadowLayer(2, 1, 1, r.getColor(R.color.white));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        linePaint.setStrokeWidth(5);
        canvas.drawLine(10,20,1000,200,linePaint);
    }

}
