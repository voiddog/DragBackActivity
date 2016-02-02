package org.voiddog.dragbackactivity.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.voiddog.dragbackactivity.util.FontLoaderUtil;


/**
 * 多动返回提示view
 * Created by Dog on 2015/11/12.
 */
public class DragBackHintView extends View implements ValueAnimator.AnimatorUpdateListener{
    static final int START_DRAG_BG = 0xff555555;

    ValueAnimator mCircleAnimator;
    //当前动画数值
    float mCurrentAnimValue = 0.0f;
    //当前的绘制区域
    int mCurrentX = 0;

    //icon开始颜色
    int mIconPositiveColor = 0xff999999;
    //icon结束颜色
    int mIconNegativeColor = 0xffffffff;
    //是否已经显示圆形
    State mState = State.NotShowCircle;
    //绘制在底部的圆形
    GradientDrawable mCircleDrawable;
    //我的文字画笔
    Paint mTextPaint = new Paint();
    //icon的id 默认显示左箭头
    String mIconId = "\uf2ea";
    //绘制的变暗层
    GradientDrawable mDarkBg;

    int ICON_SIZE = 25;
    int CIRCLE_SIZE = 75;

    public DragBackHintView(Context context) {
        super(context);
        init();
    }

    public DragBackHintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragBackHintView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init(){
        ICON_SIZE = dp2px(getContext(), ICON_SIZE);
        CIRCLE_SIZE = dp2px(getContext(), CIRCLE_SIZE);

        mCircleDrawable = new GradientDrawable();
        mCircleDrawable.setShape(GradientDrawable.OVAL);
        mCircleDrawable.setColor(0xffaaaaaa);

        if(FontLoaderUtil.getInstance() == null){
            FontLoaderUtil.init(getContext());
        }
        mTextPaint.setColor(mIconPositiveColor);
        mTextPaint.setTextSize(ICON_SIZE);
        Typeface typeface = FontLoaderUtil.getInstance().loadTextTure("Material-Design-Iconic-Font.ttf");
        if(typeface != null){
            mTextPaint.setTypeface(typeface);
        }
        mTextPaint.setAntiAlias(true);

        mCircleAnimator = new ValueAnimator();
        mCircleAnimator.setDuration(200l);
        mCircleAnimator.setInterpolator(new DecelerateInterpolator());
        mCircleAnimator.addUpdateListener(this);

        int colors[] = {START_DRAG_BG, 0x00ffffff};
        mDarkBg = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors);
        mDarkBg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mDarkBg.setShape(GradientDrawable.RECTANGLE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.clipRect(0, 0, mCurrentX, getHeight());

        //draw bg
        int alpha = 0x55 * (getWidth() - mCurrentX + getLeft()) / getWidth();
        mDarkBg.setAlpha(alpha);
        mDarkBg.setBounds(getLeft(), getTop(), getLeft() + mCurrentX, getTop() + getHeight());
        mDarkBg.draw(canvas);

        //draw circle
        int size = (int) (CIRCLE_SIZE * mCurrentAnimValue);
        if (size > 0) {
            int left = (mCurrentX - size) >> 1;
            int top = (getHeight() - size) >> 1;
            mCircleDrawable.setBounds(left, top, left + size, top + size);
            mCircleDrawable.draw(canvas);
        }

        //set paint color
        if (mCurrentAnimValue <= 1.0f) {
            int oldR = (mIconPositiveColor >> 16) & 0xff;
            int oldG = (mIconPositiveColor >> 8) & 0xff;
            int oldB = mIconPositiveColor & 0xff;

            int newR = (mIconNegativeColor >> 16) & 0xff;
            int newG = (mIconNegativeColor >> 8) & 0xff;
            int newB = mIconNegativeColor & 0xff;

            int mixR = (int) (oldR * (1 - mCurrentAnimValue) + newR * mCurrentAnimValue);
            int mixG = (int) (oldG * (1 - mCurrentAnimValue) + newG * mCurrentAnimValue);
            int mixB = (int) (oldB * (1 - mCurrentAnimValue) + newB * mCurrentAnimValue);
            mTextPaint.setColor(Color.argb(0xff, mixR, mixG, mixB));
        }
        else{
            mTextPaint.setColor(mIconNegativeColor);
        }

        //draw icon
        float value = mCurrentX * 1.0f / ICON_SIZE;
        value = value > 1.0f ? 1.0f : value;
        if (value > 0) {
            Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
            float iconWidth = mTextPaint.measureText(mIconId);
            float iconHeight = fontMetrics.bottom - fontMetrics.top;
            int startX = (int) ((mCurrentX - iconWidth) / 2);
            int startY = (int) ((getHeight() - iconHeight) / 2 - fontMetrics.top);
            int centerX = (int) (startX + iconWidth/2);
            int centerY = (int) ((getHeight() - iconHeight) / 2 + iconHeight / 2);

            mTextPaint.setAlpha((int) (value * 0xff));
            canvas.scale(value, value, centerX, centerY);
            canvas.rotate(-90 * (1.0f - value), centerX, centerY);
            canvas.drawText(mIconId, startX, startY, mTextPaint);
        }
    }

    public void setPositiveColor(int positiveColor){
        mIconPositiveColor = positiveColor;
    }

    public void setNegativeColor(int color){
        mIconNegativeColor = color;
    }

    public void setCircleColor(int color){
        mCircleDrawable.setColor(color);
    }

    public void showCircle(){
        if(mState != State.NotShowCircle){
            return;
        }
        mState = State.ShowCircle;
        mCircleAnimator.cancel();
        mCircleAnimator.setFloatValues(mCurrentAnimValue, 1.0f);
        mCircleAnimator.start();
    }

    public void hideCircle(){
        if(mState != State.ShowCircle){
            return;
        }
        mState = State.NotShowCircle;
        mCircleAnimator.cancel();
        mCircleAnimator.setFloatValues(mCurrentAnimValue, 0.0f);
        mCircleAnimator.start();
    }

    public void onDrag(int diffX){
        mCurrentX = diffX;
        invalidate();
    }

    public void onDragFinished(){
        mState = State.ExpandCircle;

        mCircleAnimator.cancel();
        float circleSize = (float) (Math.sqrt((getWidth()*getWidth()) + (getHeight()*getHeight())));
        mCircleAnimator.setFloatValues(mCurrentAnimValue, circleSize / CIRCLE_SIZE);
        mCircleAnimator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mCurrentAnimValue = (float) animation.getAnimatedValue();
        invalidate();
    }

    int dp2px(Context context, float dp){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    enum State{
        NotShowCircle,
        ShowCircle,
        ExpandCircle
    }
}
