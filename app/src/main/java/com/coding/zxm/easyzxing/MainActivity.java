package com.coding.zxm.easyzxing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.coding.libzxing.activity.CaptureActivity;
import com.coding.libzxing.encoding.QRCodeEncoder;
import com.coding.libzxing.util.Debugger;

import static com.coding.libzxing.activity.CaptureActivity.PARAMS_EXTRA_SCAN_RESULT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_SCAN = 1001;

    private Context mContext;
    private TextView mScanResultTv;
    private ImageView mQrIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initParamsAndValues();

        initViews();
    }

    private void initParamsAndValues() {
        mContext = this;

        Debugger.setLogEnable(true);
    }

    private void initViews() {
        mScanResultTv = findViewById(R.id.tv_scan_result);
        mQrIv = findViewById(R.id.iv_qr);
        findViewById(R.id.btn_scaning).setOnClickListener(this);
        findViewById(R.id.btn_get_qr).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scaning:
                Intent scan = new Intent(mContext, CaptureActivity.class);
                startActivityForResult(scan, REQUEST_CODE_SCAN);
                break;
            case R.id.btn_get_qr:
                generateQRCode();
                break;
        }
    }

    private void generateQRCode() {
        Bitmap logo = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
        Bitmap qrCode = QRCodeEncoder.createQRCode("加油皮卡丘~", 300, 300, logo);

        mQrIv.setImageBitmap(qrCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SCAN:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        final Bundle bundle = data.getExtras();
                        if (bundle != null && bundle.containsKey(PARAMS_EXTRA_SCAN_RESULT)) {
                            final String result = bundle.getString(PARAMS_EXTRA_SCAN_RESULT);

                            if (!TextUtils.isEmpty(result)) {
                                mScanResultTv.setText(result);
                            }
                        }
                    }
                }
                break;

        }
    }
}
