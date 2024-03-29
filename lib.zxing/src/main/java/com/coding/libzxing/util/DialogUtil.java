package com.coding.libzxing.util;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * Created by ZhangXinmin on 2018/12/20.
 * Copyright (c) 2018 . All rights reserved.
 */
public final class DialogUtil {
    /**
     * show message dialog
     *
     * @param context
     * @param message
     */
    public static void showDialog(@NonNull Context context, @NonNull String message,
                                  @NonNull DialogInterface.OnClickListener listener) {

        if (context == null || TextUtils.isEmpty(message) || listener == null)
            return;
        new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, listener)
                .setMessage(message)
                .setCancelable(false)
                .create()
                .show();
    }
}
