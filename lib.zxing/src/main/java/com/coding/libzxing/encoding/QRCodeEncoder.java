package com.coding.libzxing.encoding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ZhangXinmin on 2018/12/18.
 * Copyright (c) 2018 . All rights reserved.
 * Grnarate QR code
 */
public final class QRCodeEncoder {
    private QRCodeEncoder() {
    }

    /**
     * create a white-black QR Code bitmap without logo
     *
     * @param content content
     * @param size    size in pix
     * @return bitmap
     */
    public static Bitmap createCommonQRCodeNoLogo(@NonNull String content, @IntRange(from = 0) int size) {
        return createQRCodeNoLogo(content, size, Color.WHITE, Color.BLACK);
    }

    /**
     * create a white-black QR Code bitmap with a logo bitmap
     *
     * @param content content
     * @param size    size in pix
     * @return bitmap
     */
    public static Bitmap createCommonQRCodeWithLogo(@NonNull String content, @IntRange(from = 0) int size,
                                                    @NonNull Bitmap logoBm) {

        return createQRCode(content, size, size, Color.WHITE, Color.BLACK, logoBm);
    }

    /**
     * Create a QR Code bitmap without logo
     *
     * @param content   content
     * @param size      size in pix
     * @param backColor background color
     * @param foreColor foreground color
     * @return logo
     */
    public static Bitmap createQRCodeNoLogo(@NonNull String content, @IntRange(from = 0) int size,
                                            @ColorInt int backColor, @ColorInt int foreColor) {
        return createQRCode(content, size, size, backColor, foreColor, null);
    }

    /**
     * Create a QR code bitmap with logo
     *
     * @param content   content
     * @param widthPix  width in pix
     * @param heightPix height in pix
     * @param backColor background color
     * @param foreColor foreground color
     * @param logoBm    logoBm
     * @return Bitmap logo
     */
    public static Bitmap createQRCode(@NonNull String content, @IntRange(from = 0) int widthPix,
                                      @IntRange(from = 0) int heightPix, @ColorInt int backColor,
                                      @ColorInt int foreColor, @NonNull Bitmap logoBm) {

        try {
            if (TextUtils.isEmpty(content)) {
                return null;
            }


            // config params
            final Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            // 容错级别
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            //TODO:设置空白边距的宽度
            hints.put(EncodeHintType.MARGIN, 1); //default is 4
            // 图像数据转换，使用了矩阵转换
            final BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, widthPix,
                    heightPix, hints);

            final int[] pixels = new int[widthPix * heightPix];

            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (int y = 0; y < heightPix; y++) {
                for (int x = 0; x < widthPix; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * widthPix + x] = foreColor;
                    } else {
                        pixels[y * widthPix + x] = backColor;
                    }
                }
            }
            // 生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(widthPix, heightPix, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, widthPix, 0, 0, widthPix, heightPix);
            if (logoBm != null) {
                bitmap = addLogo(bitmap, logoBm);
            }
            //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * add logo in the QR code Bitmap
     */
    private static Bitmap addLogo(Bitmap src, Bitmap logo) {
        if (src == null) {
            return null;
        }
        if (logo == null) {
            return src;
        }
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();
        final int logoWidth = logo.getWidth();
        final int logoHeight = logo.getHeight();
        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }
        //logo大小为二维码整体大小的1/5
        float scaleFactor = srcWidth * 1.0f / 5 / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            e.getStackTrace();
        }
        return bitmap;
    }

    /**
     * 同步创建条形码图片
     *
     * @param content  要生成条形码包含的内容
     * @param width    条形码的宽度，单位px
     * @param height   条形码的高度，单位px
     * @param textSize 字体大小，单位px，如果等于0则不在底部绘制文字
     * @return 返回生成条形的位图
     */
    public static Bitmap syncEncodeBarcode(String content, int width, int height, int textSize) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, width, height, hints);
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    } else {
                        pixels[y * width + x] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            if (textSize > 0) {
                bitmap = showContent(bitmap, content, textSize);
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 显示条形的内容
     *
     * @param barcodeBitmap 已生成的条形码的位图
     * @param content       条形码包含的内容
     * @param textSize      字体大小，单位px
     * @return 返回生成的新条形码位图
     */
    private static Bitmap showContent(Bitmap barcodeBitmap, String content, int textSize) {
        if (TextUtils.isEmpty(content) || null == barcodeBitmap) {
            return null;
        }
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        int textWidth = (int) paint.measureText(content);
        Paint.FontMetrics fm = paint.getFontMetrics();
        int textHeight = (int) (fm.bottom - fm.top);
        float scaleRateX = barcodeBitmap.getWidth() * 1.0f / textWidth;
        if (scaleRateX < 1) {
            paint.setTextScaleX(scaleRateX);
        }
        int baseLine = barcodeBitmap.getHeight() + textHeight;
        Bitmap bitmap = Bitmap.createBitmap(barcodeBitmap.getWidth(), barcodeBitmap.getHeight() + 2 * textHeight, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas();
        canvas.drawColor(Color.WHITE);
        canvas.setBitmap(bitmap);
        canvas.drawBitmap(barcodeBitmap, 0, 0, null);
        canvas.drawText(content, barcodeBitmap.getWidth() / 2, baseLine, paint);
        canvas.save();
        canvas.restore();
        return bitmap;
    }

    /**
     * dp to px
     */
    public static int dp2px(@NonNull Context context, @FloatRange(from = 0.0) float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * px to dp
     */
    public static int px2dp(@NonNull Context context, @FloatRange(from = 0.0) float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


    /**
     * Value of sp to value of px.
     *
     * @param context context
     * @param spValue The value of sp.
     * @return value of px
     */
    public static int sp2px(@NonNull Context context, @FloatRange(from = 0.0) final float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * Value of px to value of sp.
     *
     * @param context context
     * @param pxValue The value of px.
     * @return value of sp
     */
    public static int px2sp(@NonNull Context context, @FloatRange(from = 0.0) final float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }
}
