package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
                Intent intent=new Intent(OfflineActivity.this,OfflineParentActivity.class);
                startActivity(intent);
            }
        });

        activityOfflineBinding.imageButtonBaby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(OfflineActivity.this,OfflineBabyActivity.class);
                startActivity(intent);
            }
        });

    }
}