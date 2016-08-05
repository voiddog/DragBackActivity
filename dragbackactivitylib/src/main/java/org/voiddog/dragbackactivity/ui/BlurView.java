package org.voiddog.dragbackactivity.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.voiddog.dragbackactivity.util.BlurUtil;

/**
 * Created by qigengxin on 16/8/4.
 */
public class BlurView extends View{
    private static final int MAX_BLUR_SIZE = 100;

    private boolean mIsPreDraw = false;
    private Bitmap mBlurBitmap = null;
    private Canvas mBlurCanvas = null;

    public BlurView(Context context) {
        super(context);
    }

    public BlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateBitmap(){
        View contentView = getContentView();
        if(!mIsPreDraw && mBlurBitmap != null
                && contentView != null){
            mIsPreDraw = true;

            mBlurCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            int l = getRawX(), t = getRawY();

            int saveCount = mBlurCanvas.save();
            mBlurCanvas.translate(-l, -t);
            mBlurCanvas.clipRect(l, t, l + getWidth(), t + getHeight());
            contentView.draw(mBlurCanvas);
            mBlurCanvas.restoreToCount(saveCount);

            BlurUtil.blurWithRenderScript(getContext(), mBlurBitmap, 24, true);

            mIsPreDraw = false;
        }
    }

    float x, y;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            x = event.getX();
            y = event.getY();
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE){
            setTranslationX(getTranslationX() + (event.getX() - x));
            setTranslationY(getTranslationY() + (event.getY() - y));
            updateBitmap();
            invalidate();
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(getMeasuredWidth() == 0 || getMeasuredHeight() == 0){
            return;
        }

        float scale = getScale(getMeasuredWidth(), getMeasuredHeight());
        int width = (int) (scale * getMeasuredWidth()), height = (int) (scale * getMeasuredHeight());
        if(mBlurBitmap == null || mBlurBitmap.getWidth() != width
                || mBlurBitmap.getHeight() != height){
            mBlurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mBlurCanvas = new Canvas(mBlurBitmap);
            mBlurCanvas.scale(scale, scale);
            updateBitmap();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!mIsPreDraw && mBlurBitmap != null){
            canvas.save();
            float scale = 1.0f / getScale(getWidth(), getHeight());
            canvas.scale(scale, scale);
            canvas.drawBitmap(mBlurBitmap, 0, 0, null);
            canvas.restore();
        }
    }

    private int getRawX(){
        int res = 0;
        View view = this;
        while(view != null
                && view.getId() != android.R.id.content){
            res += view.getX();
            if(view.getParent() instanceof View) {
                view = (View) view.getParent();
            }
            else{
                break;
            }
        }
        return res;
    }

    private int getRawY(){
        int res = 0;
        View view = this;
        while(view != null
                && view.getId() != android.R.id.content){
            res += view.getY();
            if(view.getParent() instanceof View) {
                view = (View) view.getParent();
            }
            else{
                break;
            }
        }
        return res;
    }

    private View getContentView(){
        View res = this;
        while(res != null && res.getId() != android.R.id.content){
            if(res.getParent() instanceof View) {
                res = (View) res.getParent();
            }
            else{
                break;
            }
        }
        return res;
    }

    private float getScale(int width, int height){
        return Math.min(
                1, Math.min(1.0f * MAX_BLUR_SIZE / width, 1.0f * MAX_BLUR_SIZE / height)
        );
    }
}
