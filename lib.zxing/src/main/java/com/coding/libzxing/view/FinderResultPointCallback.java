package com.coding.libzxing.view;


import androidx.annotation.NonNull;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * Created by ZhangXinmin on 2018/12/17.
 * Copyright (c) 2018 . All rights reserved.
 */
public class FinderResultPointCallback implements ResultPointCallback {
    private FinderView finderView;

    public FinderResultPointCallback(@NonNull FinderView finderView) {
        this.finderView = finderView;
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        finderView.addPossibleResultPoint(point);
    }
}
