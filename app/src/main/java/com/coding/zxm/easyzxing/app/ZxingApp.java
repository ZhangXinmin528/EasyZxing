package com.coding.zxm.easyzxing.app;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by ZhangXinmin on 2018/12/19.
 * Copyright (c) 2018 . All rights reserved.
 */
public class ZxingApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}
