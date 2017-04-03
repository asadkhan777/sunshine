package com.example.asadkhan.sunshine.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Brought to existence by asadkhan on 3/4/17.
 */

public class MyView extends View {

    Paint paint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    String windSpeed = "Very High";

    public MyView(Context context){
        super(context);

        AccessibilityManager accManager = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accManager.isEnabled()){
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
    }

    public MyView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
    }

    public MyView(Context context, AttributeSet attributeSet, int defaultStyle){
        super(context, attributeSet, defaultStyle);
    }

    @Override
    protected void onMeasure(int wMeasureSpec,
                             int hMeasureSpec){

        int hSpecMode = MeasureSpec.getMode(hMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(hMeasureSpec);
        int myHeight = hSpecSize;

        int wSpecMode = MeasureSpec.getMode(wMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(wMeasureSpec);
        int myWidth = wSpecSize;

        if (hSpecMode == MeasureSpec.EXACTLY){
            myHeight = hSpecSize;
        } else if (hSpecMode == MeasureSpec.AT_MOST){
            Log.e("Height is Wrapp", "hahaha");
        }

        if (wSpecMode == MeasureSpec.EXACTLY){
            myWidth = wSpecSize;
        } else if (wSpecMode == MeasureSpec.AT_MOST){
            Log.e("Width is Wrapp", "hahaha");
        }
        setMeasuredDimension(myWidth, myHeight);
    }
    @Override
    protected void onDraw(Canvas canvas){
        paint.setARGB(250, 200, 40, 50);
        canvas.drawCircle(400, 300, 100, paint);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event){
        event.getText().add(windSpeed);
        return true;
    }


}

/*
*   Stick this into any layout you want
* */

//<com.example.asadkhan.sunshine.customViews.MyView
//        android:id="@+id/canvas_view"
//        android:layout_width="300dp"
//        android:layout_height="200dp"
//        android:background="@android:color/white"
//        android:contentDescription="@string/app_name"
//        />
