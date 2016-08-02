package org.voiddog.dragbackactivity.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;

/**
 * 模糊背景用的drawee
 * Created by qigengxin on 16/8/1.
 */
public class DragBackBlurDrawee extends SimpleDraweeView{

    //当前的绘制区域
    int mCurrentX = 0;

    public DragBackBlurDrawee(Context context, GenericDraweeHierarchy hierarchy) {
        super(context, hierarchy);
    }

    public DragBackBlurDrawee(Context context) {
        super(context);
    }

    public DragBackBlurDrawee(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragBackBlurDrawee(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onDrag(int diffX){
        mCurrentX = diffX;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mCurrentX > 0){
            canvas.clipRect(0, 0, mCurrentX, getMeasuredHeight());
            super.onDraw(canvas);
        }
    }
}
