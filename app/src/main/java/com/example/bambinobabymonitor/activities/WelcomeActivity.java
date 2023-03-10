package com.example.bambinobabymonitor.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bambinobabymonitor.R;
import com.example.bambinobabymonitor.adapters.ViewPagerAdapter;
import com.example.bambinobabymonitor.databinding.ActivityWelcomeBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {
    ActivityWelcomeBinding activityWelcomeBinding;
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    ViewPagerAdapter viewPagerAdapter;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    TextView skipTextView;
    Button getStartedButton;
    long mainActivityEnd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityWelcomeBinding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        View view = activityWelcomeBinding.getRoot();
        setContentView(view);
       mainActivityEnd=getIntent().getExtras().getLong("timeMain");
        System.out.println("Main Activity' den Welcome Activity' e geçiş süresi: "+(System.currentTimeMillis()-mainActivityEnd));

        tabLayout = findViewById(R.id.tabs);
        viewPager2 = findViewById(R.id.viewPager);
        skipTextView = findViewById(R.id.skipTextView);
        getStartedButton = findViewById(R.id.getStartedButton);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser != null){ // Already have a user!
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {


            }
        }).attach();
        skipTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToLoginActivity = new Intent(WelcomeActivity.this,LoginActivity.class);
                startActivity(intentToLoginActivity);
            }
        });

    }
}