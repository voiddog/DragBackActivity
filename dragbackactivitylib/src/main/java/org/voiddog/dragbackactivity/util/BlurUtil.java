package org.voiddog.dragbackactivity.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 模糊工具类
 * Created by qigengxin on 16/8/1.
 */
public class BlurUtil {
    private static final int MAX_BLUR_IMG_SIZE = 200;
    private static Map<Activity, BitmapDrawable> S_BLUR_BGS = new WeakHashMap<>();

    public static void storeBlurBg(Activity activity){
        if(activity == null){
            return;
        }

        if(activity.getWindow().getDecorView().isShown()){
            BlurAsyncTask task = new BlurAsyncTask(activity);
            task.execute();
        }
    }

    public static BitmapDrawable getBlurBg(Activity activity){
        synchronized (BlurUtil.class){
            BitmapDrawable res = S_BLUR_BGS.get(activity);
            S_BLUR_BGS.put(activity, null);
            return res;
        }
    }

    public static int calculateSimpleSize(int srcWidth, int srcHeight,
                                          int reqWidth, int reqHeight){
        int inSampleSize = 1;

        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            float scaleW = (float) srcWidth / (float) reqWidth;
            float scaleH = (float) srcHeight / (float) reqHeight;

            float sample = scaleW > scaleH ? scaleW : scaleH;
            // 只能是2的次幂
            if (sample < 3)
                inSampleSize = (int) sample;
            else if (sample < 6.5)
                inSampleSize = 4;
            else if (sample < 8)
                inSampleSize = 8;
            else
                inSampleSize = (int) sample;

        }
        return inSampleSize;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap blurWithRenderScript(Context context, Bitmap in, float radius, boolean canReuseInBitmap){
        RenderScript rs;
        if(context instanceof Application){
            rs = RenderScript.create(context);
        }
        else{
            rs = RenderScript.create(context.getApplicationContext());
        }

        Bitmap outBitmap;

        if(canReuseInBitmap){
            outBitmap = in;
        }
        else {
            outBitmap = Bitmap.createBitmap(in.getWidth(), in.getHeight(), Bitmap.Config.ARGB_8888);
        }

        final Allocation input = Allocation.createFromBitmap(rs, in, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(outBitmap);
        rs.destroy();

        return outBitmap;
    }

    public static Bitmap getBlurImage(Bitmap in, float scaleFactor, int radius){
        Bitmap overlay = Bitmap.createBitmap((int) (in.getWidth()/scaleFactor),
                (int) (in.getHeight()/scaleFactor), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.scale(1 / scaleFactor, 1 / scaleFactor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(in, 0, 0, paint);
        overlay = fastBlur(overlay, radius, true);
        return overlay;
    }

    /**
     * 快速模糊算法
     */
    public static Bitmap fastBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    private static Bitmap getViewCache(View rootView){
        Bitmap res;

        Rect rect = new Rect();
        rootView.getWindowVisibleDisplayFrame(rect);
        rootView.destroyDrawingCache();
        rootView.setDrawingCacheEnabled(true);
        rootView.buildDrawingCache(true);
        res = rootView.getDrawingCache(true);

        /**
         * After rotation, the DecorView has no height and no width. Therefore
         * .getDrawingCache() return null. That's why we  have to force measure and layout.
         */
        if (res == null) {
            rootView.measure(
                    View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.EXACTLY)
            );
            rootView.layout(0, 0, rootView.getMeasuredWidth(),
                    rootView.getMeasuredHeight());
            rootView.destroyDrawingCache();
            rootView.setDrawingCacheEnabled(true);
            rootView.buildDrawingCache(true);
            res = rootView.getDrawingCache(true);
        }
        return res;
    }

    private static class BlurAsyncTask extends AsyncTask<Void, Void, Void>{

        private WeakReference<Activity> mBlurActivity;
        private Bitmap mBackground;

        public BlurAsyncTask(Activity activity){
            mBlurActivity = new WeakReference<Activity>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


            //retrieve background view, must be achieved on ui thread since
            //only the original thread that created a view hierarchy can touch its views.
            Activity activity = mBlurActivity.get();
            if(activity == null){
                return;
            }

            mBackground = getViewCache(activity.getWindow().getDecorView());
        }

        @Override
        protected Void doInBackground(Void... params) {
            long startTime = System.currentTimeMillis();
            if(mBackground == null || mBlurActivity.get() == null){
                return null;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mBackground.compress(Bitmap.CompressFormat.JPEG, 10, bos);

            Bitmap res;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = BlurUtil.calculateSimpleSize(mBackground.getWidth(), mBackground.getHeight(), MAX_BLUR_IMG_SIZE, MAX_BLUR_IMG_SIZE);
            byte[] outs = bos.toByteArray();

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN){
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                res = BitmapFactory.decodeByteArray(outs, 0, outs.length, options);
                BlurUtil.blurWithRenderScript(mBlurActivity.get(), res, 25, true);
            }
            else{
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                res = BitmapFactory.decodeByteArray(outs, 0, outs.length, options);
                Bitmap tmp = BlurUtil.getBlurImage(res, 2, 8);
                res.recycle();
                res = tmp;
            }

            mBackground.recycle();
            mBackground = null;

            synchronized (BlurUtil.class){
                S_BLUR_BGS.put(mBlurActivity.get(), new BitmapDrawable(mBlurActivity.get().getResources(), res));
            }
            Log.i("TAG", "获取blur所需时间: +" + (System.currentTimeMillis() - startTime));

            return null;
        }
    }
}
