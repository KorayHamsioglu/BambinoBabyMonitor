package com.example.bambinobabymonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.bambinobabymonitor.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    // Splash Activity (First Activity)
    private static int SPLASH_SCREEN_TIMEOUT = 2000;
    public static final String RTMP_BASE_URL = "rtmp://172.20.10.14:1935/live/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intentToWelcomeActivity = new Intent(MainActivity.this,WelcomeActivity.class);
                startActivity(intentToWelcomeActivity); // Going to Welcome Activity
                finish();
            }
        },SPLASH_SCREEN_TIMEOUT); // 2000ms Splashing
    }
}