package com.example.bambinobabymonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.example.bambinobabymonitor.R;

public class PerformanceActivity extends AppCompatActivity {
    private ImageButton imageButtonBabyPerformance, imageButtonParentPerformance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);
        imageButtonBabyPerformance=findViewById(R.id.imageButtonBabyPerformance);
        imageButtonParentPerformance=findViewById(R.id.imageButtonParentPerformance);

        imageButtonBabyPerformance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              Intent intent=new Intent(PerformanceActivity.this,PerformanceBabyActivity.class);
              startActivity(intent);
            }
        });
        imageButtonParentPerformance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(PerformanceActivity.this,PerformanceParentActivity.class);
                startActivity(intent);
            }
        });
    }
}