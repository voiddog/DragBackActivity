package org.voiddog.dragback;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

/**
 * 我的Application
 * Created by qigengxin on 16/8/1.
 */
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}
