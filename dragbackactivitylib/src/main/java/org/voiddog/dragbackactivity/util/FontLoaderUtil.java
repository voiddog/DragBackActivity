package org.voiddog.dragbackactivity.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.LruCache;

/**
 * 字体加载工具类
 * Created by Dog on 2015/11/8.
 */
public class FontLoaderUtil {
    static FontLoaderUtil sInstance;
    static int CacheSize = 4*1024*1024; //4MB

    public static FontLoaderUtil getInstance(){
        return sInstance;
    }

    public static void init(Context context){
        sInstance = new FontLoaderUtil(context);
    }

    public Typeface loadTextTure(String fontPath){
        Typeface typeface = mTypeFaceCache.get(fontPath);
        if(typeface == null){
            try {
                typeface = Typeface.createFromAsset(mContext.getAssets(), fontPath);
            }
            catch (Exception ignore){}
            if(typeface != null){
                mTypeFaceCache.put(fontPath, typeface);
            }
        }
        return typeface;
    }

    //上下文
    Context mContext;
    //字体缓存
    LruCache<String, Typeface> mTypeFaceCache;

    private FontLoaderUtil(Context context){
        mContext = context.getApplicationContext();
        mTypeFaceCache = new LruCache<>(CacheSize);
    }
}
