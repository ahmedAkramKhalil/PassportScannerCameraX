package com.foo.ocr;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;

public class CardLocatorView  extends View {


    Rect viewFinderRect = null ;
    ViewTreeObserver.OnDrawListener onDrawListener = null ;
    Paint paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);


    public CardLocatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    void setViewFinderRect(Rect rect){
        this.viewFinderRect = rect ;
        requestLayout();
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
    }
}
