package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.bambinobabymonitor.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding activityWelcomeBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityWelcomeBinding=ActivityWelcomeBinding.inflate(getLayoutInflater());
        View view=activityWelcomeBinding.getRoot();
        setContentView(view);

        activityWelcomeBinding.buttonGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentToWirelessActivity=new Intent(WelcomeActivity.this,WirelessActivity.class);
                startActivity(intentToWirelessActivity);
            }
        });


    }
}