package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
    private static int SPLASH_SCREEN_TIMEOUT = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intentToWelcomeActivity = new Intent(MainActivity.this,WelcomeActivity.class);
                startActivity(intentToWelcomeActivity);
            }
        },SPLASH_SCREEN_TIMEOUT);
    }
}