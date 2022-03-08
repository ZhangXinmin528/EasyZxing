package com.coding.libzxing.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.coding.libzxing.decoding.RGBLuminanceSource;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.Hashtable;

/**
 * Created by ZhangXinmin on 2018/12/20.
 * Copyright (c) 2018 . All rights reserved.
 */
public final class ZxingUtil {
    private ZxingUtil() {
        throw new UnsupportedOperationException("U con't do this!");
    }

    /**
     * get picture path
     */
    public static String getAlbumImagePath(@NonNull Context context, @NonNull Intent data) {
        String imagePath = "";
        if (context != null && data != null) {
            final Uri uri = data.getData();

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                        final String id = docId.split(":")[1];
                        final String selection = MediaStore.Images.Media._ID + "=" + id;
                        imagePath = getImagePath(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                    } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                        imagePath = getImagePath(context, contentUri, null);
                    }

                } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                    imagePath = getImagePath(context, uri, null);
                }
            } else {
                imagePath = getImagePath(context, uri, null);
            }
        }

        return imagePath;

    }

    /**
     * 通过uri和selection来获取真实的图片路径,从相册获取图片时要用
     */
    private static String getImagePath(Context context, Uri uri, String selection) {
        String path = null;
        final Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * scan the QR Code picture from album
     *
     * @param path
     * @return
     */
    public static Result scanningImage(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        final Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); //设置二维码内容的编码

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        options.inJustDecodeBounds = false; // 获取新的大小
        int sampleSize = (int) (options.outHeight / (float) 200);
        if (sampleSize <= 0) {
            sampleSize = 1;
        }
        options.inSampleSize = sampleSize;
        final Bitmap scanBitmap = BitmapFactory.decodeFile(path, options);
        final RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
        final BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        final QRCodeReader reader = new QRCodeReader();
        try {
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException | FormatException | ChecksumException e) {
            e.printStackTrace();
        } finally {
            if (scanBitmap != null && !scanBitmap.isRecycled()) {
                scanBitmap.recycle();
            }
        }
        return null;
    }
}
