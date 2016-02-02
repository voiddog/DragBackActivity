#Android高仿Chrome侧滑返回效果  
-------------------------------

在`iOS`上的`chrome`中有侧滑返回上一个页面的功能，感觉蛮好用的，刚好`Android`没有自带的侧滑返回效果，如果使用透明的`Activity`的话比较浪费性能，所以打算在实现一个简单的`DragBackActivity`，拖动的效果模仿`iOS`上的`Chrome`侧滑返回。  

效果图如下:  
![Alt text](https://raw.githubusercontent.com/qgx446738721/DragBackActivity/master/art/GIF.gif)  


**使用方法:**  
继承自`DragBackActivity`就可以有侧滑返回效果。  
如果想要禁用，重写`isDisableDrag()`函数返回`true`。
定制返回动画的色彩：

```java
//图标色，变化前
mDragLayer.setPositiveColor(...);
//图标色，变化后
mDragLayer.setNegativeColor(...);
//圆圈色
mDragLayer.setCircleColor(...);
```

**粗略解析：**  

因为需要实现的目的是继承自这个`DragBackActivity`就可以实现拖动返回的效果，因为是靠近边缘的侧滑返回，所以要用到手势处理，需要获取到当前`Activity`的根视图，手势的处理是要放在视图层处理的。  

每个`Activity`都有一个`id`为`android.R.id.content`的根视图，`setContentView`所设置的`View`就是该根视图的子`View`，本项目就是根据这个特性来移动整个`Acivity`的。  

知道怎么移动`Activity`了，接下来就只剩拦截手势和绘制返回动画了。`Android`的事件传递是根据整棵视图树来传递的，所以视图越靠近树的根就越先收到触摸事件。所以我需要在`android.R.id.content`上或者同级的地方添加自定义的`View`，这时候就需要`Window`出场了，`window`有一个方法是获取到当前窗口的根视图:  

```java
//此函数返回的是view 需要强转成view group
getWindow().getDecorView();
```

获取到窗口的根视图之后就可以往上面添加自定义视图了，手势拦截处理写在自定义视图中。废话不多说下面来看源码。  

###自定义视图
为了可扩展性我把返回动画和手势处理分开来写  

**一、返回动画的编写（`DragBackHintView`）**  
为了节省内存提高性能，我决定继承自`View`使用`Canvas`绘图绘制。  

```java
static final int START_DRAG_BG = 0xff555555;

//用于圆圈的显示、扩展与消失
ValueAnimator mCircleAnimator;
//保存当前圆圈播放动画数值
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
//icon的id 默认显示左箭头 绘制中间的箭头使用的是字体，也就是矢量图
String mIconId = "\uf2ea";
//绘制渐变的背景，阴影层
GradientDrawable mDarkBg;

int ICON_SIZE = 25;
int CIRCLE_SIZE = 75;
```

大致的变量如上述代码，绘制图标（演示中的箭头）使用的起始是字体，好处就是占用资源少，可以变色，放大不会有失真，占用内存少。圆圈、阴影采用`GradientDrawable`绘制，圆圈的展现、消失动画使用`ValueAnimator`。不知道上述类的自行谷歌。  

手势拦截处理是交给另外一个视图来处理的，所以在此处需要预留一个对外的接口来获知当前用户滑动到哪个位置了  

```java
//每当用户滑动的时候，调用此接口，触发重绘
public void onDrag(int diffX){
	mCurrentX = diffX;
	invalidate();
}

//重绘时候调用onDraw
@Override
    protected void onDraw(Canvas canvas) {
        canvas.clipRect(0, 0, mCurrentX, getHeight());

        //先绘制阴影
        int alpha = 0x55 * (getWidth() - mCurrentX + getLeft()) / getWidth();
        mDarkBg.setAlpha(alpha);
        mDarkBg.setBounds(getLeft(), getTop(), getLeft() + mCurrentX, getTop() + getHeight());
        mDarkBg.draw(canvas);

        //绘制圆圈
        int size = (int) (CIRCLE_SIZE * mCurrentAnimValue);
        if (size > 0) {
            int left = (mCurrentX - size) >> 1;
            int top = (getHeight() - size) >> 1;
            mCircleDrawable.setBounds(left, top, left + size, top + size);
            mCircleDrawable.draw(canvas);
        }

        //当用户滑动过半的时候，图标需要从一种颜色变成另一种颜色（默认白色）
        //此数值由ValueAnimator生成
        if (mCurrentAnimValue <= 1.0f) {
            int oldR = (mIconPositiveColor >> 16) & 0xff;
            int oldG = (mIconPositiveColor >> 8) & 0xff;
            int oldB = mIconPositiveColor & 0xff;

            int newR = (mIconNegativeColor >> 16) & 0xff;
            int newG = (mIconNegativeColor >> 8) & 0xff;
            int newB = mIconNegativeColor & 0xff;

			//根据比值计算中间色
            int mixR = (int) (oldR * (1 - mCurrentAnimValue) + 
            newR * mCurrentAnimValue);
            int mixG = (int) (oldG * (1 - mCurrentAnimValue) + newG * mCurrentAnimValue);
            int mixB = (int) (oldB * (1 - mCurrentAnimValue) + newB * mCurrentAnimValue);
            mTextPaint.setColor(Color.argb(0xff, mixR, mixG, mixB));
        }
        else{
	        //如果数值已经大于1了 直接设置目标色
            mTextPaint.setColor(mIconNegativeColor);
        }

        //绘制图标，绘制图标的时候需要注意 android中文字的绘制规则
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
```

上述代码就是在用户拖动的时候所经过的逻辑，绘制步骤主要分为三层，先绘制阴影背景，然后是圆圈，最后是图标。需要注意的是以下几点：  

* 圆圈出现和消失的时候，图标会变色，需要根据圆圈消失、出现的动画数值来设置图标颜色数值。
* 图标是使用字体来绘制的，需要注意在`Android`中绘制文字的时候（`drawText`）内的参数
* 图标从无到有或者从有到无，会有一个旋转缩放渐变的动画，此时的旋转和缩放的操作对象是画布  

此外还需要几个接口就是：  

```java
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

public void onDragFinished(){
    mState = State.ExpandCircle;

    mCircleAnimator.cancel();
    float circleSize = (float) (Math.sqrt((
	    getWidth()*getWidth()) + (getHeight()*getHeight())
	));
    mCircleAnimator.setFloatValues(
	    mCurrentAnimValue, circleSize / CIRCLE_SIZE);
    mCircleAnimator.start();
}
```  

**二、手势处理层（`EdgeDragLayer`）**  
关于侧滑返回的动画已经在上面的视图中处理了，在手势处理层中主要需要处理的就是手势，还有就是手指释放的时候，决定是回到最初的状态还是播放返回上一层的动画。

**手势的检测**
先看手势检测的代码  

```java
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
                if (diffX > MIN_DIS) {
                    mDragState = DragState.IsDragging;
                }
            }
            break;
        }
    }

    return mDragState == DragState.IsDragging;
}
```

在检测到手指`ACTION_DOWN`的时候，我先判断是否小于一个给定的数值`EDGE_WIDTH`，当然这个数值不是固定的，和机器的`dpi`有关。如果条件成立，把状态设为`DragState.DragStart`，之后在`ACTION_MOVE`的时候再次判断移动的距离是否达到要求，当然`MIN_DIS`也不是固定值，同样和手机屏幕像素密度有关。如果移动的距离`> MIN_DIS`了，视图层就会拦截所有触摸事件，接下来的事情就交给`onTouchEvent`来处理。  

```java
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
```

在这里面我对多点触控优化了体验，只对第一个触控点起效`mVelocityTracker`是检测手指的移动速度用的，当用户快速移动的时候，就算没有超过屏幕的一般我也应该要触发返回的事件。其它的看代码应该能懂，代（wo）码（bu）是（xiang）最（zai）好（xie）的（xia）老（qu）师（le）。  

接下去就是把这两个`View`添加到`Activity`中并简单链接一下即可。  

```java
public class DragBackActivity extends AppCompatActivity{
    private FrameLayout mRootContainer;
    protected EdgeDragLayer mDragLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!isDisableDrag()) {
            mRootContainer = (FrameLayout) findViewById(android.R.id.content);
            setupDragView();
        }
    }

    /**
     * 设置拖动手势
     */
    protected void setupDragView(){
        if(mDragLayer != null){
            return;
        }
        mDragLayer = new EdgeDragLayer(this);
        ((ViewGroup)getWindow().getDecorView()).addView(
                mDragLayer, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
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
        //移动activity中内容部分
        mRootContainer.setX(dis);
    }

    protected boolean isDisableDrag(){
        return false;
    }
}
```

源码地址: [https://github.com/qgx446738721/DragBackActivity](https://github.com/qgx446738721/DragBackActivity)  

