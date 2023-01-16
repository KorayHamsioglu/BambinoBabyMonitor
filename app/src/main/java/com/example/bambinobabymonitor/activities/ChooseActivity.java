package com.example.bambinobabymonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.bambinobabymonitor.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChooseActivity extends AppCompatActivity {
   private ImageButton imageButtonOnline, imageButtonPerformance;
   private TextView textViewLogout;
   FirebaseAuth firebaseAuth;
   FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        imageButtonOnline=findViewById(R.id.imageButtonOnline);
        imageButtonPerformance=findViewById(R.id.imageButtonPerformance);
        textViewLogout=findViewById(R.id.textViewLogout);

        firebaseAuth=FirebaseAuth.getInstance();
        firebaseUser=firebaseAuth.getCurrentUser();

        textViewLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                Intent intent=new Intent(ChooseActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


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