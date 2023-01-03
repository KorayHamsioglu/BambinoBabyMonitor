package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity {
    private static int SPLASH_SCREEN_TIMEOUT = 2000;
    public static final String RTMP_BASE_URL = "rtmp://192.168.1.102:1935/live/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intentToWelcomeActivity = new Intent(MainActivity.this,WelcomeActivity.class);
                startActivity(intentToWelcomeActivity);
                finish();
            }
        },SPLASH_SCREEN_TIMEOUT);
    }
}