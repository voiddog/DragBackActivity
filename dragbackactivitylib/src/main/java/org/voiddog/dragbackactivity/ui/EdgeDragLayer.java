package org.voiddog.dragbackactivity.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * 左侧拖动的控制层
 * Created by Dog on 2015/11/7.
 */
public class EdgeDragLayer extends ViewGroup{

    //默认动画时间为300毫秒
    static final long ANIM_TIME = 300l;
    //大于这个速度就开始finish
    static final int MIN_SPEED = 300;

    int MIN_DIS = 8;
    int EDGE_WIDTH = 50;

    //开始的触摸点
    Point mStartPoint = new Point(0, 0);
    //拖动状态
    DragState mDragState = DragState.DragCancel;
    //多动监听
    DragListener mDragListener;
    //屏幕参数
    DisplayMetrics mDisplayMetrics;
    //结束动画，返回动画
    ValueAnimator mFinishAnim, mCancelAnim;
    //第一个触控点
    int mFirstPointId = -1;
    //手指速度检测
    VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    //指示器view
    DragBackHintView mHintView;
    // 模糊背景 fresco view
    DragBackBlurImageView mBlurBg;
    // 是否设置了模糊背景
    boolean mHasSetBlurBg = false;
    int mCurrentOffsetX = 0;

    public EdgeDragLayer(Context context) {
        super(context);
        init();
    }

    public EdgeDragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EdgeDragLayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        EDGE_WIDTH = dp2px(getContext(), EDGE_WIDTH);
        MIN_DIS = dp2px(getContext(), MIN_DIS);

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(mDisplayMetrics);

        mFinishAnim = new ValueAnimator();
        mFinishAnim.addUpdateListener(new FinishAnimController());
        mFinishAnim.setInterpolator(new DecelerateInterpolator());
        mFinishAnim.setDuration(ANIM_TIME);

        mCancelAnim = new ValueAnimator();
        mCancelAnim.addUpdateListener(new CancelAnimController());
        mCancelAnim.setInterpolator(new DecelerateInterpolator());
        mCancelAnim.setDuration(ANIM_TIME);

        mBlurBg = new DragBackBlurImageView(getContext());
        mBlurBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(mBlurBg, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mHintView = new DragBackHintView(getContext());
        addView(mHintView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        updateArrowUI(0);
    }

    public void setPositiveColor(int positiveColor){
        mHintView.setPositiveColor(positiveColor);
    }

    public void setNegativeColor(int color){
        mHintView.setNegativeColor(color);
    }

    public void setCircleColor(int color) {
        mHintView.setCircleColor(color);
    }

    public void setOnDragListener(DragListener listener){
        mDragListener = listener;
    }

    public boolean hasSetBlurBg(){
        return mHasSetBlurBg;
    }

    public void setBlurBg(Drawable drawable){
        if(drawable == null){
            return;
        }
        mHasSetBlurBg = true;
        mBlurBg.setImageDrawable(drawable);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if(mDragState == DragState.PlayAnim){
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mStartPoint.set((int)event.getX(), (int)event.getY());
                mFirstPointId = event.getPointerId(0);
                if(mStartPoint.x < EDGE_WIDTH) {
                    mDragState = DragState.DragStart;
                }
                else{
                    mDragState = DragState.DragCancel;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if(mDragState == DragState.DragStart){
                    int diffX = (int)(event.getX() - mStartPoint.x);
                    int diffY = (int)(event.getY() - mStartPoint.y);
                    if (diffX > MIN_DIS && diffY < MIN_DIS) {
                        mDragState = DragState.IsDragging;
                    }
                }
                break;
            }
        }

        return mDragState == DragState.IsDragging;
    }

    @Override
    protected void onDraw(Canvas canvas) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mDragState == DragState.PlayAnim){
            return true;
        }

        if(event.getPointerId(event.getActionIndex()) != mFirstPointId){
            return mDragState != DragState.DragCancel;
        }

        mVelocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (mDragState == DragState.DragStart) {
                    int diffX = (int) (event.getX() - mStartPoint.x);
                    if (diffX > MIN_DIS) {
                        mDragState = DragState.IsDragging;
                    }
                } else if (mDragState == DragState.IsDragging) {
                    dispatchDragEvent((int) (event.getX() - mStartPoint.x));
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mDragState == DragState.IsDragging) {
                    int diffX = (int) (event.getX() - mStartPoint.x);
                    //需要结束
                    if (needFinished(event)) {
                        playFinishAnim(diffX);
                        //扩展circle
                        mHintView.onDragFinished();
                    } else {
                        playCancelAnim(diffX);
                    }
                }
                break;
            }
        }

        return mDragState != DragState.DragCancel;
    }

    /**
     * 判断是否需要结束当前页面
     */
    boolean needFinished(MotionEvent event){
        mVelocityTracker.computeCurrentVelocity(1000);
        int speedX = px2dip(getContext(), mVelocityTracker.getXVelocity());
        if(speedX > MIN_SPEED){
            return true;
        }
        else if(event.getX() > mDisplayMetrics.widthPixels/2){
            return true;
        }
        return false;
    }

    /**
     * 播放退出动画
     */
    public void playFinishAnim(int currentDiffX){
        mCancelAnim.cancel();
        mFinishAnim.cancel();
        mDragState = DragState.PlayAnim;

        float p = currentDiffX * 1.0f / mDisplayMetrics.widthPixels;

        mFinishAnim.setFloatValues(p, 1.0f);
        mFinishAnim.start();
    }

    /**
     * 播放取消动画
     */
    public void playCancelAnim(int currentDiffX){
        mCancelAnim.cancel();
        mFinishAnim.cancel();
        mDragState = DragState.PlayAnim;

        float p = currentDiffX * 1.0f / mDisplayMetrics.widthPixels;

        mCancelAnim.setFloatValues(p, 0.0f);
        mCancelAnim.start();
    }

    //更新返回指示器
    void updateArrowUI(int offsetX) {
        mHintView.onDrag(offsetX);
        mBlurBg.onDrag(offsetX);
        if(offsetX > getWidth()/2){
            mHintView.showCircle();
        }
        else{
            mHintView.hideCircle();
        }
    }

    class FinishAnimController implements ValueAnimator.AnimatorUpdateListener{

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float value = (float) animation.getAnimatedValue();
            dispatchDragEvent((int) (mDisplayMetrics.widthPixels * value));
            if(value == 1.0f){
                mDragState = DragState.DragBackEnd;
                if(mDragListener != null){
                    mDragListener.onDragBackEnd();
                }
            }
        }
    }

    class CancelAnimController implements ValueAnimator.AnimatorUpdateListener{

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float value = (float) animation.getAnimatedValue();
            dispatchDragEvent((int) (mDisplayMetrics.widthPixels * value));
            if(value == 0.0f){
                mDragState = DragState.DragCancel;
                if(mDragListener != null){
                    mDragListener.onCancelDrag();
                }
            }
        }
    }

    /**
     * 发布拖动事件
     */
    void dispatchDragEvent(int dis){
        if(mDragListener != null){
            mDragListener.onDragEvent(dis);
        }
        mCurrentOffsetX = dis;
        updateArrowUI(dis);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /**
         * 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式
         */
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 计算出所有的childView的宽和高
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        /**
         * 如果是wrap_content设置为我们计算的值
         * 否则：直接设置为父容器计算的值
         */
        setMeasuredDimension((widthMode == MeasureSpec.EXACTLY) ? sizeWidth
                : sizeWidth, (heightMode == MeasureSpec.EXACTLY) ? sizeHeight
                : sizeHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for(int i = getChildCount() - 1; i >= 0; --i){
            getChildAt(i).layout(l, t, l + getMeasuredWidth(), t + getMeasuredHeight());
        }
    }

    public interface DragListener {
        //拖动事件
        void onDragEvent(int dis);
        //取消事件
        void onCancelDrag();
        //拖动返回成功
        void onDragBackEnd();
    }

    int px2dip(Context context, float pxValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(pxValue / scale + 0.5f);
    }

    int dp2px(Context context, float dp){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    enum DragState{
        DragStart,
        IsDragging,
        DragCancel,
        PlayAnim,
        DragBackEnd
    }
}
