package com.example.android.uamp.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.example.android.uamp.R;


/**
 * A squared ImageVIew
 * Created by Jorge on 21/06/2014.
 */
public class SquaredImageView extends ImageView {

    private static final int MEASURE_WITH_WIDTH = 0;
    private static final int MEASURE_WITH_HEIGHT = 1;
    private int measureWith;

    public SquaredImageView(Context context) {
        super(context);
        measureWith = MEASURE_WITH_WIDTH;
    }

    public SquaredImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode())
            parseAttributes(context, attrs);
    }

    public SquaredImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode())
            parseAttributes(context, attrs);
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.mSquareView);
        measureWith = values.getInt(R.styleable.mSquareView_measure_with, MEASURE_WITH_WIDTH);
        values.recycle();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        switch (measureWith) {
            case MEASURE_WITH_WIDTH:
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
                break;
            case MEASURE_WITH_HEIGHT:
                super.onMeasure(heightMeasureSpec, heightMeasureSpec);
                break;
        }
    }
}
