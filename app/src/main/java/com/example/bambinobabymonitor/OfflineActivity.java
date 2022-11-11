package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.example.bambinobabymonitor.databinding.ActivityOfflineBinding;

public class OfflineActivity extends AppCompatActivity {

    private ActivityOfflineBinding activityOfflineBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityOfflineBinding=ActivityOfflineBinding.inflate(getLayoutInflater());
        View view=activityOfflineBinding.getRoot();
        setContentView(view);


        activityOfflineBinding.imageButtonParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        activityOfflineBinding.imageButtonBaby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }
}