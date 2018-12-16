package com.coding.libzxing.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.coding.libzxing.R;
import com.coding.libzxing.camera.CameraManager;
import com.google.zxing.ResultPoint;

import java.util.Collection;
import java.util.HashSet;

import static android.graphics.PixelFormat.OPAQUE;

/**
 * Created by ZhangXinmin on 2018/12/16.
 * Copyright (c) 2018.
 */
public class FinderView extends View {
    protected static final String TAG = FinderView.class.getSimpleName();

    private static final int[]  SCANNER_ALPHA                 =   {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long   ANIMATION_DELAY               =   10L;
    private static final int    OPAQUE                        =   0xFF;
    private static final int    CORNER_RECT_WIDTH             =   8;  //扫描区边角的宽
    private static final int    CORNER_RECT_HEIGHT            =   40; //扫描区边角的高
    private static final int    SCANNER_LINE_MOVE_DISTANCE    =   5;  //扫描线移动距离
    private static final int    SCANNER_LINE_HEIGHT           =   10;  //扫描线宽度

    private Context mContext;

    private Paint mPaint;
    private Bitmap resultBitmap;


    //模糊区域颜色
    private int maskColor;
    private int resultColor;
    //扫描区域边框颜色
    private int frameColor;
    //扫描线颜色
    private int laserColor;
    //四角颜色
    private int cornerColor;
    //扫描点的颜色
    private int resultPointColor;
    private int scannerAlpha;
    //扫描区域提示文本
    private String labelText;
    //扫描区域提示文本颜色
    private int labelTextColor;
    //扫描区域提示文字字号
    private float labelTextSize;

    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;

    public static int scannerStart = 0;
    public static int scannerEnd = 0;


    public FinderView(Context context) {
        this(context, null, 0);
    }

    public FinderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FinderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initParamsAndValues(context, attrs);

    }

    //init params and values
    private void initParamsAndValues(@NonNull Context context, @NonNull AttributeSet attrs) {
        mContext = context;

        if (attrs != null) {
            @SuppressLint({"Recycle"})
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FinderView);
            laserColor = array.getColor(R.styleable.FinderView_laser_color, 0x00FF00);
            cornerColor = array.getColor(R.styleable.FinderView_corner_color, 0x00FF00);
            frameColor = array.getColor(R.styleable.FinderView_frame_color, 0xFFFFFF);
            resultPointColor = array.getColor(R.styleable.FinderView_result_point_color, 0xC0FFFF00);
            maskColor = array.getColor(R.styleable.FinderView_mask_color, 0x60000000);
            resultColor = array.getColor(R.styleable.FinderView_result_color, 0xB0000000);
            labelTextColor = array.getColor(R.styleable.FinderView_label_text_color, 0x90FFFFFF);
            labelText = array.getString(R.styleable.FinderView_label_text);
            labelTextSize = array.getFloat(R.styleable.FinderView_label_text_size, 36f);
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        scannerAlpha = 0;
        possibleResultPoints = new HashSet<>(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        if (scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top;
            scannerEnd = frame.bottom;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas, frame, width, height);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            mPaint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, mPaint);
        } else {
            // Draw a two pixel solid black border inside the framing rect
            drawFrame(canvas, frame);
            // 绘制边角
            drawCorner(canvas, frame);
            //绘制提示信息
            drawTextInfo(canvas, frame);
            // Draw a red "laser scanner" line through the middle to show decoding is active
            drawLaserScanner(canvas, frame);

            Collection<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new HashSet<ResultPoint>(5);
                lastPossibleResultPoints = currentPossible;
                mPaint.setAlpha(OPAQUE);
                mPaint.setColor(resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, mPaint);
                }
            }
            if (currentLast != null) {
                mPaint.setAlpha(OPAQUE / 2);
                mPaint.setColor(resultPointColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, mPaint);
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            //指定重绘区域，该方法会在子线程中执行
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }

    }

    /**
     * 绘制模糊区域
     * raw the exterior (i.e. outside the framing rect) darkened
     *
     * @param canvas
     * @param frame
     * @param width
     * @param height
     */
    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        mPaint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, mPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, mPaint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, mPaint);
        canvas.drawRect(0, frame.bottom + 1, width, height, mPaint);
    }

    /**
     * 绘制扫描区边框
     * Draw a two pixel solid black border inside the framing rect
     *
     * @param canvas
     * @param frame
     */
    private void drawFrame(Canvas canvas, Rect frame) {
        mPaint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, mPaint);
        canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, mPaint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, mPaint);
        canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, mPaint);
    }

    /**
     * 绘制边角
     * @param canvas
     * @param frame
     */
    private void drawCorner(Canvas canvas, Rect frame) {
        mPaint.setColor(cornerColor);
        //左上
        canvas.drawRect(frame.left, frame.top,
                frame.left + CORNER_RECT_WIDTH, frame.top + CORNER_RECT_HEIGHT, mPaint);
        canvas.drawRect(frame.left, frame.top,
                frame.left + CORNER_RECT_HEIGHT, frame.top + CORNER_RECT_WIDTH, mPaint);
        //右上
        canvas.drawRect(frame.right - CORNER_RECT_WIDTH, frame.top,
                frame.right, frame.top + CORNER_RECT_HEIGHT, mPaint);
        canvas.drawRect(frame.right - CORNER_RECT_HEIGHT, frame.top,
                frame.right, frame.top + CORNER_RECT_WIDTH, mPaint);
        //左下
        canvas.drawRect(frame.left, frame.bottom - CORNER_RECT_WIDTH,
                frame.left + CORNER_RECT_HEIGHT, frame.bottom, mPaint);
        canvas.drawRect(frame.left, frame.bottom - CORNER_RECT_HEIGHT,
                frame.left + CORNER_RECT_WIDTH, frame.bottom, mPaint);
        //右下
        canvas.drawRect(frame.right - CORNER_RECT_WIDTH, frame.bottom - CORNER_RECT_HEIGHT,
                frame.right, frame.bottom, mPaint);
        canvas.drawRect(frame.right - CORNER_RECT_HEIGHT, frame.bottom - CORNER_RECT_WIDTH,
                frame.right, frame.bottom, mPaint);
    }

    /**绘制文本
     *
     * @param canvas
     * @param frame
     */
    private void drawTextInfo(Canvas canvas, Rect frame) {
        mPaint.setColor(labelTextColor);
        mPaint.setTextSize(labelTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(labelText, frame.left + frame.width() / 2, frame.top - CORNER_RECT_HEIGHT, mPaint);
    }

    /**
     * 绘制扫描线
     * @param canvas
     * @param frame
     */
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        mPaint.setColor(laserColor);
        //扫描线闪烁效果
//    paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
//    scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//    int middle = frame.height() / 2 + frame.top;
//    canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
        //线性渐变
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + SCANNER_LINE_HEIGHT,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        RadialGradient radialGradient = new RadialGradient(
                (float)(frame.left + frame.width() / 2),
                (float)(scannerStart + SCANNER_LINE_HEIGHT / 2),
                360f,
                laserColor,
                shadeColor(laserColor),
                Shader.TileMode.MIRROR);

        SweepGradient sweepGradient = new SweepGradient(
                (float)(frame.left + frame.width() / 2),
                (float)(scannerStart + SCANNER_LINE_HEIGHT),
                shadeColor(laserColor),
                laserColor);

        ComposeShader composeShader = new ComposeShader(radialGradient, linearGradient, PorterDuff.Mode.ADD);

        mPaint.setShader(radialGradient);
        if(scannerStart <= scannerEnd) {
            //矩形
//      canvas.drawRect(frame.left, scannerStart, frame.right, scannerStart + SCANNER_LINE_HEIGHT, paint);
            //椭圆
            RectF rectF = new RectF(frame.left + 2 * SCANNER_LINE_HEIGHT, scannerStart, frame.right - 2 * SCANNER_LINE_HEIGHT, scannerStart + SCANNER_LINE_HEIGHT);
            canvas.drawOval(rectF, mPaint);
            scannerStart += SCANNER_LINE_MOVE_DISTANCE;
        } else {
            scannerStart = frame.top;
        }
        mPaint.setShader(null);
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }


    //处理颜色模糊
    private int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "20"+hax.substring(2);
        return Integer.valueOf(result, 16);
    }
}
