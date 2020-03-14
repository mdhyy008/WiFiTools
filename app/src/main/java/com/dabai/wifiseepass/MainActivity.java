package com.dabai.wifiseepass;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dabai.wifiseepass.activitys.ScanToolActivity;
import com.dabai.wifiseepass.activitys.SettingsActivity;
import com.dabai.wifiseepass.databinding.ActivityMainBinding;
import com.dabai.wifiseepass.databinding.ItemWifiBinding;
import com.dabai.wifiseepass.utils.WifiInfo;
import com.dabai.wifiseepass.utils.WifiManage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding amb;


    private boolean isroot;
    private WifiManage wifiManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        amb = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(amb.getRoot());

        suthread();
    }

    //菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.scanner:
                startActivity(new Intent(this, ScanToolActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void Init() throws Exception {
        final List<WifiInfo> wifiInfos = WifiManage.Read();

        amb.lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            private String text;

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WifiInfo wifiInfo = wifiInfos.get(i);

                String ssid = wifiInfo.getSsid();
                String pass = wifiInfo.getPassword();

                if (pass.equals("无密码")) {
                    text = "WIFI:T:;P:;S:" + ssid + ";";
                } else {
                    text = "WIFI:T:WPA;P:" + pass + ";S:" + ssid + ";";
                }

                //转二维码工作
                ToRes(text, ssid, pass);

            }
        });


        amb.lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                ClipboardManager clipboardManager = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mclipData = ClipData.newPlainText("Label", "WiFi名称 : " + wifiInfos.get(i).ssid + "\n密码 : " + wifiInfos.get(i).password);
                clipboardManager.setPrimaryClip(mclipData);

                Toast.makeText(MainActivity.this, "复制完成", Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        WifiAdapter ad = new WifiAdapter(this, R.layout.item_wifi, wifiInfos);
        amb.lv.setAdapter(ad);

    }

    // 用来计算返回键的点击间隔时间
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                //弹出提示，可以有多种方式
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void ToRes(String text, String name, String pass) {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_shareqr, null);

        MaterialDialog md1 = new MaterialDialog.Builder(this)
                .customView(view, false)
                .show();

        TextView te1 = view.findViewById(R.id.textView3);
        TextView te2 = view.findViewById(R.id.textView4);
        ImageView img1 = view.findViewById(R.id.imageView2);

        te1.setText("名称:" + name);
        te2.setText("密码:" + pass);

        //生成二维码设置到 img1

        Bitmap bit = createQRCodeBitmap(text, 700, 700, "UTF-8", "H", "1", Color.BLACK, Color.WHITE);

        img1.setImageBitmap(bit);

    }

    /**
     * 生成简单二维码
     *
     * @param content                字符串内容
     * @param width                  二维码宽度
     * @param height                 二维码高度
     * @param character_set          编码方式（一般使用UTF-8）
     * @param error_correction_level 容错率 L：7% M：15% Q：25% H：35%
     * @param margin                 空白边距（二维码与边框的空白区域）
     * @param color_black            黑色色块
     * @param color_white            白色色块
     * @return BitMap
     */
    public static Bitmap createQRCodeBitmap(String content, int width, int height,
                                            String character_set, String error_correction_level,
                                            String margin, int color_black, int color_white) {
        // 字符串内容判空
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        // 宽和高>=0
        if (width < 0 || height < 0) {
            return null;
        }
        try {
            /** 1.设置二维码相关配置 */
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            // 字符转码格式设置
            if (!TextUtils.isEmpty(character_set)) {
                hints.put(EncodeHintType.CHARACTER_SET, character_set);
            }
            // 容错率设置
            if (!TextUtils.isEmpty(error_correction_level)) {
                hints.put(EncodeHintType.ERROR_CORRECTION, error_correction_level);
            }
            // 空白边距设置
            if (!TextUtils.isEmpty(margin)) {
                hints.put(EncodeHintType.MARGIN, margin);
            }
            /** 2.将配置参数传入到QRCodeWriter的encode方法生成BitMatrix(位矩阵)对象 */
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            /** 3.创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值 */
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    //bitMatrix.get(x,y)方法返回true是黑色色块，false是白色色块
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = color_black;//黑色色块像素设置
                    } else {
                        pixels[y * width + x] = color_white;// 白色色块像素设置
                    }
                }
            }
            /** 4.创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,并返回Bitmap对象 */
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }


    /**
     * 检查root权限
     */
    public void suthread() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                isroot = isRoot();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (!isroot) {
                            new MaterialDialog.Builder(MainActivity.this)
                                    .title("权限提示")
                                    .content("ROOT权限获取失败,点击重新获取。")
                                    .positiveText("尝试获取")
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            try {
                                                Runtime.getRuntime().exec("su");
                                            } catch (IOException e) {
                                            }
                                            suthread();
                                        }
                                    })
                                    .negativeText("退出软件")
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            finish();
                                        }
                                    })
                                    .cancelable(false)
                                    .show();
                        } else {
                            //拿到权限，为所欲为。
                            wifiManage = new WifiManage();
                            try {
                                Init();
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "初始化异常", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        }).start();

    }

    /**
     * 检查root权限
     *
     * @return
     */
    private boolean isRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();

            int i = process.waitFor();
            if (0 == i) {
                process = Runtime.getRuntime().exec("su");
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;

    }

    /**
     * 运行su命令
     *
     * @param command
     * @return
     */
    private String exec(String command) {
        try {
            java.lang.Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public class WifiAdapter extends ArrayAdapter {

        List<WifiInfo> wifiInfos = null;
        Context context;
        private final int resourceId;

        ItemWifiBinding iwb;

        public WifiAdapter(Context context, int textViewResourceId, List<WifiInfo> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            resourceId = textViewResourceId;
            this.wifiInfos = objects;
        }


        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return wifiInfos.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return wifiInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            convertView = LayoutInflater.from(context).inflate(R.layout.item_wifi, null);

            iwb = ItemWifiBinding.bind(convertView);

            iwb.textView.setText("WiFi名称 : " + wifiInfos.get(position).ssid);
            iwb.textView2.setText("密码 : " + wifiInfos.get(position).password);

            if (wifiInfos.get(position).password.equals("无密码")) {
                iwb.imageView.setImageResource(R.drawable.wifi2);
            } else {
                iwb.imageView.setImageResource(R.drawable.wifilock2);
            }

            return convertView;
        }

    }


    public String TAG = "dabaizzz";
}
