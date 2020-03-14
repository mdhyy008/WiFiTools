package com.dabai.wifiseepass.activitys;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.dabai.wifiseepass.R;
import com.dabai.wifiseepass.utils.WifiAdmin;

import java.io.IOException;

import cn.simonlee.xcodescanner.core.CameraScanner;
import cn.simonlee.xcodescanner.core.GraphicDecoder;
import cn.simonlee.xcodescanner.core.NewCameraScanner;
import cn.simonlee.xcodescanner.core.OldCameraScanner;
import cn.simonlee.xcodescanner.core.ZBarDecoder;
import cn.simonlee.xcodescanner.view.AdjustTextureView;

public class ScanToolActivity extends AppCompatActivity implements CameraScanner.CameraListener, TextureView.SurfaceTextureListener, GraphicDecoder.DecodeListener, View.OnClickListener {

    private AdjustTextureView mTextureView;
    private View mScannerFrameView;
    private CameraScanner mCameraScanner;
    protected GraphicDecoder mGraphicDecoder;
    protected String TAG = "XCodeScanner";
    private ImageButton mButton_Flash;
    private int[] mCodeType;

    TextView sc_text;
    CardView sc_card;


    AlertDialog resdia;
    private String result_end;
    private Vibrator vibrator;
    private ProgressDialog pd;
    private AlertDialog md3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_tool);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // getSupportActionBar().setElevation(0);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mTextureView = findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        mScannerFrameView = findViewById(R.id.scannerframe);

        mButton_Flash = findViewById(R.id.btn_flash);
        mButton_Flash.setOnClickListener(this);

        resdia = new AlertDialog.Builder(this).setTitle("结果").setMessage("正在过滤数据")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (result_end.contains("WIFI")){
                            ToResult(result_end);
                            resdia.hide();
                        }else {
                            Toast.makeText(ScanToolActivity.this, "不包含WiFi信息!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).create();

        /*
         * 注意，SDK21的设备是可以使用NewCameraScanner的，但是可能存在对新API支持不够的情况，比如红米Note3（双网通Android5.0.2）
         * 开发者可自行配置使用规则，比如针对某设备型号过滤，或者针对某SDK版本过滤
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCameraScanner = new NewCameraScanner(this);
        } else {
            mCameraScanner = new OldCameraScanner(this);
        }

        int checkResult1 = getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.CAMERA);

        wifiAdmin = new WifiAdmin(getApplicationContext());

        pd = new ProgressDialog(ScanToolActivity.this);
        pd.setTitle("提示");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 1) {
            startActivity(new Intent(this, getClass()));
            finish();

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onRestart() {
        if (mTextureView.isAvailable()) {
            //部分机型转到后台不会走onSurfaceTextureDestroyed()，因此isAvailable()一直为true，转到前台后不会再调用onSurfaceTextureAvailable()
            //因此需要手动开启相机
            mCameraScanner.setPreviewTexture(mTextureView.getSurfaceTexture());
            mCameraScanner.setPreviewSize(mTextureView.getWidth(), mTextureView.getHeight());
            mCameraScanner.openCamera(this.getApplicationContext());
        }
        super.onRestart();
    }

    @Override
    protected void onPause() {
        mCameraScanner.closeCamera();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mCameraScanner.setGraphicDecoder(null);
        if (mGraphicDecoder != null) {
            mGraphicDecoder.setDecodeListener(null);
            mGraphicDecoder.detach();
        }
        mCameraScanner.detach();
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                // 处理返回逻辑
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCameraScanner.setPreviewTexture(surface);
        mCameraScanner.setPreviewSize(width, height);
        mCameraScanner.openCamera(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // TODO 当View大小发生变化时，要进行调整。
//        mTextureView.setImageFrameMatrix();
//        mCameraScanner.setPreviewSize(width, height);
//        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override// 每有一帧画面，都会回调一次此方法
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void openCameraSuccess(int frameWidth, int frameHeight, int frameDegree) {
        mTextureView.setImageFrameMatrix(frameWidth, frameHeight, frameDegree);
        if (mGraphicDecoder == null) {
            // mGraphicDecoder = new DebugZBarDecoder(this, mCodeType);//使用带参构造方法可指定条码识别的格式
            mGraphicDecoder = new ZBarDecoder(this);
        }
        //该区域坐标为相对于父容器的左上角顶点。
        //TODO 应考虑TextureView与ScannerFrameView的Margin与padding的情况
        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
        mCameraScanner.setGraphicDecoder(mGraphicDecoder);
    }

    @Override
    public void openCameraError() {
        Toast.makeText(this, "出错了", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void noCameraPermission() {

        /**
         * 申请权限
         */
        int checkResult1 = getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.CAMERA);
        //if(!=允许),抛出异常
        if (checkResult1 != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(this).setTitle("权限申请")
                    .setMessage("如果没有相机权限，二维码扫描是不能工作的!")
                    .setCancelable(false)
                    .setNeutralButton("拒绝", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setPositiveButton("授权", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1); // 动态申请读取权限
                            }
                        }
                    })
                    .show();

        }

    }

    @Override
    public void cameraDisconnected() {
        Toast.makeText(this, "断开连接", Toast.LENGTH_SHORT).show();

    }


    @Override
    public void cameraBrightnessChanged(int brightness) {

    }

    int mCount = 0;
    String mResult = null;

    void ToResult(String data) {

        if (data != null) {
            String result = data;
            try {
                res_linkwifi(result);
            } catch (Exception e) {
                Toast.makeText(this, "不是WiFi码!", Toast.LENGTH_SHORT).show();
            }

        }
    }


    public boolean isNetworkOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("ping -c 3 www.baidu.com");
            int exitValue = ipProcess.waitFor();
            Log.i("Avalible", "Process:" + exitValue);
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


    private String password, netWorkType, netWorkName;
    private WifiAdmin wifiAdmin;

    public void res_linkwifi(String restext) throws Exception {

        String passwordTemp = restext.substring(restext
                .indexOf("P:"));
        password = passwordTemp.substring(2,
                passwordTemp.indexOf(";"));
        String netWorkTypeTemp = restext.substring(restext
                .indexOf("T:"));
        netWorkType = netWorkTypeTemp.substring(2,
                netWorkTypeTemp.indexOf(";"));
        String netWorkNameTemp = restext.substring(restext
                .indexOf("S:"));
        netWorkName = netWorkNameTemp.substring(2,
                netWorkNameTemp.indexOf(";"));

        md3 = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setCancelable(false)
                .setMessage("确定连接到 [" + netWorkName + "] 嘛?\n\nWiFi名称:" + netWorkName + "\n密码:" + password + "\n加密方式:" + netWorkType)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        pd.setMessage("正在连接 - " + netWorkName);

                        pd.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                if (!wifiAdmin.mWifiManager.isWifiEnabled()) {
                                    wifiAdmin.openWifi();
                                }

                                int net_type = 0x13;
                                if (netWorkType
                                        .compareToIgnoreCase("wpa") == 0) {
                                    net_type = WifiAdmin.TYPE_WPA;// wpa
                                } else if (netWorkType
                                        .compareToIgnoreCase("wep") == 0) {
                                    net_type = WifiAdmin.TYPE_WEP;// wep
                                } else {
                                    net_type = WifiAdmin.TYPE_NO_PASSWD;// 无加密
                                }

                                wifiAdmin.addNetwork(netWorkName, password, net_type);

                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {

                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (isNetworkOnline()) {
                                            pd.dismiss();
                                            new AlertDialog.Builder(ScanToolActivity.this)
                                                    .setMessage("连接成功了呢O(∩_∩)O").setTitle("提示").setCancelable(false)
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            finish();
                                                        }
                                                    })
                                                    .show();
                                        } else {
                                            pd.dismiss();
                                            new AlertDialog.Builder(ScanToolActivity.this)
                                                    .setMessage("现在好像不能上网呦(T_T)").setTitle("提示").setCancelable(false)
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            finish();
                                                        }
                                                    }).show();
                                        }


                                    }
                                });

                            }
                        }).start();


                    }
                }).setNeutralButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();


    }


    @Override
    public void decodeComplete(String result, int type, int quality, int requestCode) {
        if (result == null) return;
        if (result.equals(mResult)) {
            if (!resdia.isShowing()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        vibrator.vibrate(100);
                    }
                }).start();

                resdia.show();


            }
            if (++mCount > 1) {//连续四次相同则显示结果（主要过滤脏数据，也可以根据条码类型自定义规则）
                if (quality < 10) {
                    result_end = result;
                    if (!result.contains("WIFI")){
                        resdia.setMessage("不是WiFi码!");
                    }else {
                        resdia.setMessage(result);
                    }
                } else if (quality < 100) {
                    result_end = result;
                    if (!result.contains("WIFI")){
                        resdia.setMessage("不是WiFi码!");
                    }else {
                        resdia.setMessage(result);
                    }
                } else {
                    result_end = result;
                    if (!result.contains("WIFI")){
                        resdia.setMessage("不是WiFi码!");
                    }else {
                        resdia.setMessage(result);
                    }
                }
            }
        } else {
            mCount = 1;
            mResult = result;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_flash: {
                if (v.isSelected()) {
                    v.setSelected(false);
                    mCameraScanner.closeFlash();
                } else {
                    v.setSelected(true);
                    mCameraScanner.openFlash();
                }
                break;
            }
        }
    }


}
