package com.dabai.wifiseepass;

import android.app.Application;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;


public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //更新 自定义初始化
        Beta.autoInit = true;
        //自定义 通知栏小图标
        Beta.smallIconId = R.drawable.update1;

        //bugly初始化
        Bugly.init(getApplicationContext(), "c5fab40e6d", false);

    }
}
