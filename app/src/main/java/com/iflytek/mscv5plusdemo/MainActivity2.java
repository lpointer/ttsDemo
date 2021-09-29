package com.iflytek.mscv5plusdemo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.iflytek.wheelview.OnWheelChangedListener;
import com.iflytek.wheelview.WheelView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity2 extends Activity {
    private WheelView wheelY;
    private WheelView wheelM;
    private WheelView wheelD;
    private WheelView wheelH;
    private WheelView wheelMm;
    private TextView date;
    private TextView back;

    private final Calendar calendar = Calendar.getInstance();
    private static final Set<Integer> MONTH_OF_31 = new HashSet();
    private static final Set<Integer> MONTH_OF_30 = new HashSet();

    static {
        MONTH_OF_31.addAll(Arrays.asList(1, 3, 5, 7, 8, 10, 12));
        MONTH_OF_30.addAll(Arrays.asList(4, 6, 9, 11));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wheelY = findViewById(R.id.wheelY);
        wheelM = findViewById(R.id.wheelM);
        wheelD = findViewById(R.id.wheelD);
        wheelH = findViewById(R.id.wheelH);
        wheelMm = findViewById(R.id.wheelMm);
        date = findViewById(R.id.date);
        back = findViewById(R.id.back);

        wheelY.setRange(1970, 2300);
        //wheelM.setRange(1, 12);中英文
        wheelH.setRange(0, 23);
        wheelMm.setRange(0, 59);

        setupDayOfMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));

        wheelY.postDelayed(() -> {
            updateTime();
            setupDayOfMonth(wheelY.getCurrentValue(), wheelM.getCurrentValue());
        }, 10);

        wheelY.setOnWheelChangedListener(onWheelChangedListener);
        wheelM.setOnWheelChangedListener(onWheelChangedListener);
        wheelD.setOnWheelChangedListener(onWheelChangedListener);
        wheelH.setOnWheelChangedListener(onWheelChangedListener);
        wheelMm.setOnWheelChangedListener(onWheelChangedListener);
        TtsManager.getInstance(MainActivity2.this).init();
        autoSetTime();
        date.setText("当前时间 ：" + new Date());
        back.setOnClickListener(view -> finish());
    }

    private void setupDayOfMonth(int year, int month) {
        boolean isLeapYear = TimeUtils.isLeapYear(year);//判断是否是闰年
        if (MONTH_OF_31.contains(month)) {
            wheelD.setRange(1, 31);
        } else if (MONTH_OF_30.contains(month)) {
            wheelD.setRange(1, 30);
        } else {//二月
            if (isLeapYear) {
                wheelD.setRange(1, 29);
            } else {
                wheelD.setRange(1, 28);
            }
        }
    }

    public void updateTime(Date date) {
        Log.v("view", "updateTime:" + date.getYear());
        wheelY.setCurrentIndex(date.getYear() + 1900 - 1970);
        wheelM.setCurrentIndex(date.getMonth());
        wheelD.setCurrentIndex(date.getDate() - 1);
        wheelH.setCurrentIndex(date.getHours());
        wheelMm.setCurrentIndex(date.getMinutes());
    }

    public void updateTime() {
        wheelY.setCurrentIndex(calendar.get(Calendar.YEAR) - 1970);
        wheelM.setCurrentIndex(calendar.get(Calendar.MONTH));
        wheelD.setCurrentIndex(calendar.get(Calendar.DAY_OF_MONTH) - 1);
        wheelH.setCurrentIndex(calendar.get(Calendar.HOUR_OF_DAY));
        wheelMm.setCurrentIndex(calendar.get(Calendar.MINUTE));
    }


    private final OnWheelChangedListener onWheelChangedListener = new OnWheelChangedListener() {
        public void onChanged(WheelView view, int oldIndex, int newIndex) {
            if (!view.scrolling()) {//不生效
                int value = view.getCurrentValue();
                String tip = "";
                switch (view.getId()) {
                    case R.id.wheelY:
                        tip = value + "年";
                        break;
                    case R.id.wheelM:
                        tip = value + "月";
                        break;
                    case R.id.wheelD:
                        tip = value + "日";
                        break;
                    case R.id.wheelH:
                        tip = value + "时";
                        break;
                    case R.id.wheelMm:
                        tip = value + "分";
                        break;
                }

                if (!TextUtils.isEmpty(tip)) {
                    TtsManager.getInstance(MainActivity2.this).startLocalSpeaking(tip);
                }
            }

            setupDayOfMonth(wheelY.getCurrentValue(), wheelM.getCurrentValue());
            setTime();
        }
    };

    private void autoSetTime() {
        new Thread() {
            public void run() {
                try {
                    if (!NetworkUtils.isAvailable()) {
                        Log.v("view", "NetworkUtils.isAvailable false");
                        return;
                    }
                    final Date date = getWebsiteDatetime("http://www.baidu.com");
                    if (date != null) {
                        wheelY.post(() -> {
                            updateTime(date);
                            setupDayOfMonth(wheelY.getCurrentValue(), wheelM.getCurrentValue());
//                            setTime();
                        });
                    } else {
                        Log.v("view", "updateTime: date == null");
                    }
                } catch (Exception e) {
                    Log.v("view", "updateTime:" + e.getMessage());
                }
            }
        }.start();
    }

    private static Date getWebsiteDatetime(String webUrl) {
        try {
            URL url = new URL(webUrl);// 取得资源对象
            URLConnection uc = url.openConnection();// 生成连接对象
            uc.connect();// 发出连接
            long ld = uc.getDate();// 读取网站日期时间
            return new Date(ld);
        } catch (MalformedURLException e) {
            Log.v("view", "MalformedURLException:" + e.getMessage());
        } catch (IOException e) {
            Log.v("view", "IOException:" + e.getMessage());
        }
        return null;
    }

    private void setTime() {
        String result = execRootCmds("date \""
                + wheelM.getCurrentStr()
                + wheelD.getCurrentStr()
                + wheelH.getCurrentStr()
                + wheelMm.getCurrentStr()
                + wheelY.getCurrentStr()
                + ".00\" set"
                + "\n busybox hwclock -w\n");

        Log.v("view", result);

        boolean isSuccess = result.equals("");
        Log.d("MainActivity2", "setTime: 当前时间戳" + System.currentTimeMillis());
        Log.d("MainActivity2", "setTime: 当前时间" + new Date());
        date.setText("当前时间 ：" + new Date());
    }

    public static String execRootCmds(String cmd) {
        StringBuilder result = new StringBuilder();
        BufferedReader bufferedReader = null;
        DataOutputStream dos = null;
        String receive;

        try {
            Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();

            while ((receive = bufferedReader.readLine()) != null) {
                result.append("\n").append(receive);
            }

            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result.toString();
    }
}