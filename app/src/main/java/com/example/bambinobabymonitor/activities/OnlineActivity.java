package com.example.bambinobabymonitor.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.bambinobabymonitor.R;
import com.example.bambinobabymonitor.baby.BabyActivity;
import com.example.bambinobabymonitor.parent.ParentActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class OnlineActivity extends AppCompatActivity {
    private ImageButton imageButtonParent,imageButtonBaby;
    private ImageView imageViewBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);

        imageButtonBaby=findViewById(R.id.imageButtonBaby);
        imageButtonParent=findViewById(R.id.imageButtonParent);
        imageViewBack=findViewById(R.id.back_button_online);

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

        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(OnlineActivity.this,ChooseActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}