package org.voiddog.dragbackactivity;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.voiddog.dragbackactivity.ui.EdgeDragLayer;
import org.voiddog.dragbackactivity.util.BlurUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 拖动返回Activity
 * Created by qgx44 on 2016/2/2.
 */
public class DragBackActivity extends AppCompatActivity{
    private static List<WeakReference<Activity> > sActivities = new ArrayList<>();

    private FrameLayout mRootContainer;
    protected EdgeDragLayer mDragLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivities.add(new WeakReference<Activity>(this));
        mRootContainer = (FrameLayout) findViewById(android.R.id.content);
        if(!isDisableDrag()) {
            setupDragView();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing()){
            BlurUtil.storeBlurBg(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sActivities.remove(sActivities.size() - 1);
    }

    /**
     * 设置拖动手势
     */
    protected void setupDragView(){
        if(mDragLayer != null){
            return;
        }
        mDragLayer = new EdgeDragLayer(this);
        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        ViewGroup linearParent = (ViewGroup) decorView.getChildAt(0);

        decorView.removeView(linearParent);
        decorView.addView(
                mDragLayer, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        mDragLayer.addView(linearParent, 0);
        mDragLayer.setOnDragListener(new EdgeDragLayer.DragListener() {
            @Override
            public void onDragEvent(int dis) {
                dragEvent(dis);
            }

            @Override
            public void onCancelDrag() {
            }

            @Override
            public void onDragBackEnd() {
                customFinish();
            }
        });
    }

    void customFinish(){
        finish();
        overridePendingTransition(R.anim.drag_activity_enter_anim, R.anim.drag_activity_exit_anim);
    }

    void dragEvent(int dis){
        if(dis < 0){
            dis = 0;
        }
        if(!mDragLayer.hasSetBlurBg() && sActivities.size() > 1){
            mDragLayer.setBlurBg(BlurUtil.getBlurBg(sActivities.get(sActivities.size() - 2).get()));
        }
        mRootContainer.setX(dis);
    }

    protected boolean isDisableDrag(){
        return false;
    }
}
