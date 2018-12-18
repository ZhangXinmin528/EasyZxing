package com.coding.libzxing.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.coding.libzxing.R;
import com.coding.libzxing.camera.CameraManager;
import com.coding.libzxing.decoding.CaptureActivityHandler;
import com.coding.libzxing.decoding.InactivityTimer;
import com.coding.libzxing.decoding.RGBLuminanceSource;
import com.coding.libzxing.util.Debugger;
import com.coding.libzxing.util.PermissionChecker;
import com.coding.libzxing.view.FinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.util.Hashtable;
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
    private static final int REQUEST_WRITE_EXTERNAL = 2000;

    private String[] PERMISSION_WRITE_EXTERNAL = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

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
    private String photo_path;

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
            if (PermissionChecker.checkPersmission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                PermissionChecker.requestPermissions(this, PERMISSION_WRITE_EXTERNAL, REQUEST_WRITE_EXTERNAL);
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

    /**
     * 扫描二维码图片的方法
     * @param path
     * @return
     */
    public Result scanningImage(String path) {
        if(TextUtils.isEmpty(path)){
            return null;
        }
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); //设置二维码内容的编码

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        scanBitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false; // 获取新的大小
        int sampleSize = (int) (options.outHeight / (float) 200);
        if (sampleSize <= 0){
            sampleSize = 1;
        }
        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(path, options);
        RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        try {
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_EXTERNAL) {
            Debugger.e(TAG + "state:" + (grantResults[0] != PackageManager.PERMISSION_GRANTED));
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(mContext)
                        .setMessage(getString(R.string.tips_album_permission_deny))
                        .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {

                        })
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (requestCode==RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SCAN_GALLERY:
                    //获取选中图片的路径
                    Cursor cursor = getContentResolver().query(data.getData(), null, null, null, null);
                    if (cursor.moveToFirst()) {
                        photo_path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    }
                    cursor.close();

                    mProgress = new ProgressDialog(CaptureActivity.this);
                    mProgress.setMessage("正在扫描...");
                    mProgress.setCancelable(false);
                    mProgress.show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Result result = scanningImage(photo_path);
                            if (result != null) {
                                Intent resultIntent = new Intent();
                                Bundle bundle = new Bundle();
                                bundle.putString(PARAMS_EXTRA_SCAN_RESULT,result.getText());
                                resultIntent.putExtras(bundle);
                                setResult(RESULT_OK, resultIntent);
                            } else {
                                Message m = handler.obtainMessage();
                                m.what = R.id.decode_failed;
                                m.obj = "Scan failed!";
                                handler.sendMessage(m);
                            }
                        }
                    }).start();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
