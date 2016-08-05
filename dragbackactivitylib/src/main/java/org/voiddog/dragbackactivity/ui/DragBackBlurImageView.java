package org.voiddog.dragbackactivity.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 模糊背景用的drawee
 * Created by qigengxin on 16/8/1.
 */
public class DragBackBlurImageView extends ImageView{

    //当前的绘制区域
    int mCurrentX = 0;

    public DragBackBlurImageView(Context context) {
        super(context);
    }

    public DragBackBlurImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragBackBlurImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onDrag(int diffX){
        mCurrentX = diffX;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mCurrentX > 0){
            int saveCount = canvas.save();
            int translateX = getWidth() >> 2;
            translateX = (int) (translateX * (1 - 1.0f * mCurrentX/getWidth()));
            canvas.translate(-translateX, 0);
            canvas.clipRect(translateX, 0, mCurrentX + translateX, getHeight());
            super.onDraw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }
}
