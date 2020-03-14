package com.dabai.wifiseepass.activitys;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dabai.wifiseepass.R;
import com.dabai.wifiseepass.utils.DabaiUtils;
import com.dabai.wifiseepass.utils.DateUtils;
import com.dabai.wifiseepass.utils.FileUtils;
import com.dabai.wifiseepass.utils.WifiAdmin;
import com.dabai.wifiseepass.utils.WifiInfo;
import com.dabai.wifiseepass.utils.WifiManage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Context context;
        private String TAG = "dabaizzz";
        private WifiAdmin wifiAdmin;
        private MaterialDialog backdia;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            context = getContext();
            //version init  版本号初始化
            final Preference ver = getPreferenceManager().findPreference("other_version");

            //change preference version name;
            ver.setSummary(new DabaiUtils().getVersionName(context));
            wifiAdmin = new WifiAdmin(context);

        }


        @Override
        public boolean onPreferenceTreeClick(Preference preference) {

            switch (preference.getKey()) {

                case "other_feedback":
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("https://support.qq.com/product/134723"));
                    startActivity(intent);
                    break;
                case "other_version":
                    Intent intent2 = new Intent(Intent.ACTION_VIEW);
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent2.setData(Uri.parse("https://www.coolapk.com/apk/com.dabai.wifiseepass"));
                    startActivity(intent2);
                    break;

                case "other_share":
                    new DabaiUtils().shareFromCoolapk(context);
                    break;

                case "other_backup":
                    View view = LayoutInflater.from(context).inflate(R.layout.dialog_huifu,null);

                    backdia =  new MaterialDialog.Builder(context)
                            .title("备份空间")
                            .customView(view, false)
                            .positiveText("创建备份")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        List<WifiInfo> wifiInfos = WifiManage.Read();
                                        String jsonString = JSON.toJSONString(wifiInfos);

                                        File file = new File(context.getExternalFilesDir("backup"),"WifiInfos_"+DateUtils.getTime(3) +".json");
                                        new FileUtils().writeText(file.getAbsolutePath(),jsonString,true);

                                        if (file.exists()){
                                            Toast.makeText(context, "创建备份成功:"+file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(context, "备份失败!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .show();

                    final File dir = context.getExternalFilesDir("backup");

                    ArrayAdapter adapter = new ArrayAdapter(context,android.R.layout.simple_list_item_1,dir.list());
                    ListView lv = view.findViewById(R.id.lv);
                    lv.setAdapter(adapter);


                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            huifuWifis(dir.listFiles()[position].getAbsolutePath());
                        }
                    });

                    break;

            }
                return super.onPreferenceTreeClick(preference);
        }

        /**
         * 恢复 WiFi配置
         * @param s
         */
        private void huifuWifis(String s) {

            try {
                JSONArray jsonArray = JSONArray.parseArray(new DabaiUtils().readSDFile(s));

                for (Object ob: jsonArray) {
                    JSONObject jo = (JSONObject) ob;
                    String ssid = jo.getString("ssid");
                    String pass = jo.getString("password");

                    if (pass.equals("无密码")){
                        wifiAdmin.addNetwork(ssid, pass, WifiAdmin.TYPE_NO_PASSWD);
                    }else {
                        wifiAdmin.addNetwork(ssid, pass, WifiAdmin.TYPE_WPA);
                    }

                    Toast.makeText(context, "恢复"+jsonArray.size()+"个WiFi信息完成!", Toast.LENGTH_SHORT).show();
                    backdia.dismiss();
                }
            } catch (IOException e) {
                Toast.makeText(context, "恢复失败!", Toast.LENGTH_SHORT).show();
                backdia.dismiss();
            }


        }
    }

}