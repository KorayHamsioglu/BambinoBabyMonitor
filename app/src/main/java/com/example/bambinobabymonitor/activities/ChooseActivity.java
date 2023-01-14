package com.example.bambinobabymonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.example.bambinobabymonitor.R;

public class ChooseActivity extends AppCompatActivity {
   private ImageButton imageButtonOnline, imageButtonPerformance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        imageButtonOnline=findViewById(R.id.imageButtonOnline);
        imageButtonPerformance=findViewById(R.id.imageButtonPerformance);


        imageButtonOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ChooseActivity.this,OnlineActivity.class);
                startActivity(intent);
            }
        });

        imageButtonPerformance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ChooseActivity.this,PerformanceActivity.class);
                startActivity(intent);
            }
        });
    }
}