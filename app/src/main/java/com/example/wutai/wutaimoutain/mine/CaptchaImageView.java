package com.example.wutai.wutaimoutain.mine;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.wutai.wutaimoutain.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CaptchaImageView extends View {
    private String TAG="CapthaImageView";
    private int mWidth;//控件的宽度
    private int mHeight;//控件的高度
    private ArrayList<PointF> mPoints = new ArrayList<PointF>();//干扰点坐标的集合
    private Random mRandom = new Random();;
    private ArrayList<Path> mPaths = new ArrayList<Path>();//     绘制贝塞尔曲线的路径集合
    private int mCodeCount;//验证码的位数
    private int mTextSize;//验证码字符的大小
    private float mTextWidth;//验证码字符显示的宽度
    private String mCodeString;//随机数字和字母
    private Paint mTextPaint;//文字画笔
    private Paint mPointPaint;//干扰点画笔
    private Paint mPathPaint;//干扰线画笔
    private boolean isNeedNewData;
    private ArrayList<Integer> rotateDegree;
    private ArrayList<Map<String,Integer>>textRGB;
    private ArrayList<Map<String,Integer>>lineRGB;
    public CaptchaImageView(Context context) {
        this(context,null);
    }
    /**
     * 在xml布局文件中使用view但没有指定style的时候调用
     * @param context
     * @param attrs
     */
    public CaptchaImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    /**
     * 在xml布局文件中使用view并指定style的时候调用
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public CaptchaImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttrValues(context, attrs);
        init();
    }

    private void getAttrValues(Context context,AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CaptchaImageView);
        mCodeCount = typedArray.getInteger(R.styleable.CaptchaImageView_codeCount, 5); // 获取布局中验证码位数属性值，默认为5个
        // 获取布局中验证码文字的大小，默认为20sp
        mTextSize = (int) typedArray.getDimension(R.styleable.CaptchaImageView_textSize,
                typedArray.getDimensionPixelSize(R.styleable.CaptchaImageView_textSize,
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics())));
        // 一个好的习惯是用完资源要记得回收，就想打开数据库和IO流用完后要记得关闭一样
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = measureWidth(widthMeasureSpec);
        int measureHeight = measureHeight(heightMeasureSpec);

        // 其实这个方法最终会调用setMeasuredDimension(int measureWidth,int measureHeight);
        // 将测量出来的宽高设置进去完成测量
        setMeasuredDimension(measureWidth, measureHeight);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        // 初始化数据
        if(isNeedNewData){
            initData();
        }
        int length = mCodeString.length();
        float charLength = mWidth/length;
        for(int i=1;i<=length;i++){
            // 这里只会产生0和1，如果是1那么正旋转正角度，否则旋转负角度
            int offsetDegree = rotateDegree.get(i-1);
            canvas.save();
            canvas.rotate(offsetDegree, (i-1) * charLength+charLength/2, mHeight-30);
            // 给画笔设置随机颜色
            mTextPaint.setARGB(255,textRGB.get(i-1).get("R"),textRGB.get(i-1).get("G"), textRGB.get(i-1).get("B"));
            canvas.drawText(String.valueOf(mCodeString.charAt(i - 1)), (i-1) * charLength+charLength/2, mHeight-30, mTextPaint);
            canvas.restore();
        }

        // 产生干扰效果1 -- 干扰点
        for(PointF pointF : mPoints){
            mPointPaint.setARGB(255,mRandom.nextInt(200)+20,mRandom.nextInt(200)+20,mRandom.nextInt(200)+20);
            canvas.drawPoint(pointF.x,pointF.y,mPointPaint);
        }

        // 产生干扰效果2 -- 干扰线
        for(int i=0;i<mPaths.size();i++){
            Path path = mPaths.get(i);
            Map<String,Integer> map =lineRGB.get(i);
            mPathPaint.setARGB(255,map.get("R"),map.get("G"),map.get("B"));
            canvas.drawPath(path, mPathPaint);
        }
    }
    private void initData() {
        // 获取控件的宽和高，此时已经测量完成
        mHeight = getHeight();
        mWidth = getWidth();
        mPoints.clear();
        // 生成干扰点坐标
        for(int i=0;i<100;i++){
            PointF pointF = new PointF(mRandom.nextInt(mWidth),mRandom.nextInt(mHeight));
            mPoints.add(pointF);
        }

        mPaths.clear();
        // 生成干扰线坐标
        for(int i=0;i<3;i++){
            Path path = new Path();
            int startX = mRandom.nextInt(mWidth);
            int startY = mRandom.nextInt(mHeight);
            int endX = mRandom.nextInt(mWidth);
            int endY = mRandom.nextInt(mHeight);
            path.moveTo(startX,startY);
            path.quadTo(Math.abs(endX-startX)/2,Math.abs(endY-startY)/2,endX,endY);
            mPaths.add(path);
        }

        //生成旋转角度
        if(rotateDegree==null){
            rotateDegree = new ArrayList<>(mCodeString.length()+1);
        }
        rotateDegree.clear();
        for(int i=0;i<mCodeString.length();i++){
            int offsetDegree = mRandom.nextInt(30);
            // 这里只会产生0和1，如果是1那么正旋转正角度，否则旋转负角度
            offsetDegree = mRandom.nextInt(2) == 1?offsetDegree:-offsetDegree;
            rotateDegree.add(offsetDegree);
        }
        if(textRGB==null){
            textRGB=new ArrayList<>(mCodeString.length()+1);
        }
        textRGB.clear();
        for(int i=0;i<mCodeString.length();i++){
            Map<String,Integer> map = new HashMap<>(4);
            map.put("R",mRandom.nextInt(200) + 20);
            map.put("G",mRandom.nextInt(200) + 20);
            map.put("B",mRandom.nextInt(200) + 20);
            textRGB.add(map);
        }
        if(lineRGB==null){
            lineRGB=new ArrayList<>(mCodeString.length()+1);
        }
        lineRGB.clear();
        for(int i=0;i<3;i++){
            Map<String,Integer> map = new HashMap<>(4);
            map.put("R",mRandom.nextInt(200) + 20);
            map.put("G",mRandom.nextInt(200) + 20);
            map.put("B",mRandom.nextInt(200) + 20);
            lineRGB.add(map);
        }

        isNeedNewData=false;
    }

    private void init() {
        isNeedNewData =true;
        // 生成随机数字和字母组合
        mCodeString = getCharAndNumr(mCodeCount);
        // 初始化文字画笔
        mTextPaint = new Paint();
        mTextPaint.setStrokeWidth(3); // 画笔大小为3
        mTextPaint.setTextSize(mTextSize); // 设置文字大小
        Log.e(TAG, "init: mTextSize"+mTextSize);
        // 初始化干扰点画笔
        mPointPaint = new Paint();
        mPointPaint.setStrokeWidth(6);
        mPointPaint.setStrokeCap(Paint.Cap.ROUND); // 设置断点处为圆形

        // 初始化干扰线画笔
        mPathPaint = new Paint();
        mPathPaint.setStrokeWidth(5);
        mPathPaint.setColor(Color.GRAY);
        mPathPaint.setStyle(Paint.Style.STROKE); // 设置画笔为空心
        mPathPaint.setStrokeCap(Paint.Cap.ROUND); // 设置断点处为圆形

        // 取得验证码字符串显示的宽度值
        mTextWidth = mTextPaint.measureText(mCodeString);

        rotateDegree=new ArrayList<>(mCodeString.length()+1);
        textRGB = new ArrayList<>(mCodeString.length()+1);
        lineRGB = new ArrayList<>(3+1);
    }
    /**
     * java生成随机数字和字母组合
     * @param length 生成随机数的长度
     * @return
     */
    public static String getCharAndNumr(int length) {
        String val = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            // 输出字母还是数字
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            // 字符串
            if ("char".equalsIgnoreCase(charOrNum)) {
                // 取得大写字母还是小写字母
                int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val += (char) (choice + random.nextInt(26));
            } else if ("num".equalsIgnoreCase(charOrNum)) { // 数字
                val += String.valueOf(random.nextInt(10));
            }
        }
        return val;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                // 重新生成随机数字和字母组合
                mCodeString = getCharAndNumr(mCodeCount);
                isNeedNewData=true;
                invalidate();
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }
    /**
     * 测量宽度
     * @param widthMeasureSpec
     */
    private int measureWidth(int widthMeasureSpec) {
        int result = (int) (mTextWidth*1.8f);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if(widthMode == MeasureSpec.EXACTLY){
            // 精确测量模式，即布局文件中layout_width或layout_height一般为精确的值或match_parent
            result = widthSize; // 既然是精确模式，那么直接返回测量的宽度即可
        }else{
            if(widthMode == MeasureSpec.AT_MOST) {
                // 最大值模式，即布局文件中layout_width或layout_height一般为wrap_content
                result = Math.min(result,widthSize);
            }
        }
        return result;
    }
    /**
     * 测量高度
     * @param heightMeasureSpec
     */
    private int measureHeight(int heightMeasureSpec) {
        int result = (int) (mTextWidth/1.6f);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if(heightMode == MeasureSpec.EXACTLY){
            // 精确测量模式，即布局文件中layout_width或layout_height一般为精确的值或match_parent
            result = heightSize; // 既然是精确模式，那么直接返回测量的宽度即可
        }else{
            if(heightMode == MeasureSpec.AT_MOST) {
                // 最大值模式，即布局文件中layout_width或layout_height一般为wrap_content
                result = Math.min(result,heightSize);
            }
        }
        return result;
    }
    /**
     * 获取验证码字符串，进行匹配的时候只需要字符串比较即可（具体比较规则自己决定）
     * @return 验证码字符串
     */
    public String getCodeString() {
        return mCodeString;
    }







}
