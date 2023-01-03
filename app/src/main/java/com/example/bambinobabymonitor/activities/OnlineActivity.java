package com.example.bambinobabymonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.example.bambinobabymonitor.R;
import com.example.bambinobabymonitor.baby.BabyActivity;
import com.example.bambinobabymonitor.parent.ParentActivity;

public class OnlineActivity extends AppCompatActivity {
    private ImageButton imageButtonParent,imageButtonBaby;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);

        imageButtonBaby=findViewById(R.id.imageButtonBaby);
        imageButtonParent=findViewById(R.id.imageButtonParent);

        imageButtonParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(OnlineActivity.this, ParentActivity.class);
                startActivity(intent);
            }
        });

        imageButtonBaby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(OnlineActivity.this, BabyActivity.class);
                startActivity(intent);
            }
        });
    }
}