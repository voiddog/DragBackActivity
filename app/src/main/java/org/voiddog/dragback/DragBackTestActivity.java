package org.voiddog.dragback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import org.voiddog.dragbackactivity.DragBackActivity;

/**
 * 测试拖动返回的活动
 * Created by qgx44 on 2016/2/2.
 */
public class DragBackTestActivity extends DragBackActivity{

    //View
    Toolbar toolbar;
    Button btn_start_drag_back;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //箭头色
        mDragLayer.setPositiveColor(getResources().getColor(R.color.colorPrimaryDark));
        //圈圈色
        mDragLayer.setCircleColor(getResources().getColor(R.color.colorPrimaryDark));

        setContentView(R.layout.activity_drag_back_test);
        findViews();
        setupViews();
    }

    void findViews(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        btn_start_drag_back = (Button) findViewById(R.id.btn_start_drag_back);
    }

    void setupViews(){
        btn_start_drag_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DragBackTestActivity.this, DragBackTestActivity.class);
                startActivity(intent);
            }
        });
    }
}
