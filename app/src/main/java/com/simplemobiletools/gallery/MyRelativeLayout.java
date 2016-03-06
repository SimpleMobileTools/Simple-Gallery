package com.simplemobiletools.gallery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class MyRelativeLayout extends RelativeLayout {
    private Drawable mForegroundSelector;

    public MyRelativeLayout(Context context) {
        super(context);
    }

    public MyRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        mForegroundSelector = getResources().getDrawable(R.drawable.selector);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mForegroundSelector.setState(getDrawableState());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mForegroundSelector.setBounds(0, 0, w, h);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        mForegroundSelector.draw(canvas);
    }
}
