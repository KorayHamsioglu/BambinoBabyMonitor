package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.bambinobabymonitor.databinding.ActivityWirelessBinding;

public class WirelessActivity extends AppCompatActivity {
    private ActivityWirelessBinding activityWirelessBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityWirelessBinding=ActivityWirelessBinding.inflate(getLayoutInflater());
        View view=activityWirelessBinding.getRoot();
        setContentView(view);

        activityWirelessBinding.imageButtonOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(WirelessActivity.this,LoginActivity.class);
                startActivity(intent);

            }
        });

        activityWirelessBinding.imageButtonOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    }
}