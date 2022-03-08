package com.coding.libzxing.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coding.libzxing.R;
import com.coding.libzxing.camera.CameraManager;
import com.coding.libzxing.decoding.CaptureActivityHandler;
import com.coding.libzxing.decoding.InactivityTimer;
import com.coding.libzxing.util.Debugger;
import com.coding.libzxing.util.DialogUtil;
import com.coding.libzxing.util.PermissionChecker;
import com.coding.libzxing.util.ZxingUtil;
import com.coding.libzxing.view.FinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.Vector;

/**
 * Created by ZhangXinmin on 2018/12/17.
 * Copyright (c) 2018 . All rights reserved.
 * Activity for scaning
 */
public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        MediaPlayer.OnCompletionListener, View.OnClickListener {

    public static final String PARAMS_EXTRA_SCAN_RESULT = "qr_scan_result";
    private static final String TAG = CaptureActivity.class.getSimpleName();

    //select picture from album
    private static final int REQUEST_CODE_SCAN_GALLERY = 1000;

    private Context mContext;

    private FinderView mFinderView;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;

    private CaptureActivityHandler handler;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;

    //sound effect for scaning
    private boolean playBeep;
    private MediaPlayer mediaPlayer;
    private boolean vibrate;

    //scaning from album
    private Bitmap scanBitmap;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        initParamsAndValues();
        initViews();
    }

    private void initParamsAndValues() {
        mContext = this;

        CameraManager.init(getApplicationContext());
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);

    }

    private void initViews() {
        mFinderView = findViewById(R.id.finderview_capture);
        findViewById(R.id.iv_arrow_back).setOnClickListener(this);
        findViewById(R.id.tv_scan_album).setOnClickListener(this);

        checkPermissons();
    }

    /**
     * check camera permission
     */
    private void checkPermissons() {
        if (!PermissionChecker.checkPersmission(mContext, Manifest.permission.CAMERA)) {
            DialogUtil.showDialog(mContext, getString(R.string.all_check_camera_permission),
                    (dialog, which) -> CaptureActivity.this.finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SurfaceView surfaceView = findViewById(R.id.surfaceview_capture);
        final SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        final AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService != null &&
                audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }

        initBeepSound();
        vibrate = true;
    }

    //init camera
    private void initCamera(@NonNull SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException | RuntimeException ioe) {
            Debugger.e(ioe.getMessage());
            return;
        }

        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    //MediaPlayer play sound when scaning
    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //set callback when the end of a media source has been reached.
            mediaPlayer.setOnCompletionListener(this);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                //Sets the volume on this player
                mediaPlayer.setVolume(0.1f, 0.1f);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.seekTo(0);
    }

    /**
     * Handle scan result
     *
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        final String resultString = result.getText();
        Log.e(TAG, "decode result : " + resultString);
        Toast.makeText(mContext, "result:" + resultString, Toast.LENGTH_SHORT).show();
        if (!TextUtils.isEmpty(resultString)) {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString(PARAMS_EXTRA_SCAN_RESULT, resultString);
            resultIntent.putExtras(bundle);
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            Debugger.e(TAG + "..decode result is empty or null!");
        }
        finish();
    }

    /**
     * play scan sound and strike vibrate
     */
    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }

        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(200);
        }
    }

    public FinderView getFinderView() {
        return mFinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onClick(View v) {
        final int viewId = v.getId();
        if (viewId == R.id.iv_arrow_back) {
            finish();
        } else if (viewId == R.id.tv_scan_album) {
            if (!PermissionChecker.checkPersmission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogUtil.showDialog(mContext, getString(R.string.all_check_extronal_permission),
                        (dialog, which) -> CaptureActivity.this.finish());
            } else {
                openAlbum();
            }
        }
    }

    /**
     * open album
     */
    private void openAlbum() {
        //open album
        Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        innerIntent.setType("image/*");
        Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
        startActivityForResult(wrapperIntent, REQUEST_CODE_SCAN_GALLERY);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SCAN_GALLERY:
                    if (data != null) {
                        final Uri uri = data.getData();
                        if (uri != null) {
                            final String imagePath = ZxingUtil.getAlbumImagePath(mContext, data);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final Result result = ZxingUtil.scanningImage(imagePath);
                                    if (result != null) {
                                        Intent resultIntent = new Intent();
                                        Bundle bundle = new Bundle();
                                        bundle.putString(PARAMS_EXTRA_SCAN_RESULT, result.getText());
                                        resultIntent.putExtras(bundle);
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    } else {
                                        Message m = handler.obtainMessage();
                                        m.what = R.id.decode_failed;
                                        m.obj = "Scan failed!";
                                        handler.sendMessage(m);
                                    }
                                }
                            }).start();
                        }
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
