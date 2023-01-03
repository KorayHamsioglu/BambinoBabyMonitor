package com.example.bambinobabymonitor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.bambinobabymonitor.databinding.ActivityWelcomeBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding activityWelcomeBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityWelcomeBinding=ActivityWelcomeBinding.inflate(getLayoutInflater());
        View view=activityWelcomeBinding.getRoot();
        setContentView(view);

        TabLayout tabLayout=findViewById(R.id.tabs);
        ViewPager2 viewPager2=findViewById(R.id.viewPager);


       ViewPagerAdapter adapter=new ViewPagerAdapter(this);
       viewPager2.setAdapter(adapter);

       new TabLayoutMediator(tabLayout, viewPager2,
               new TabLayoutMediator.TabConfigurationStrategy() {
                   @Override
                   public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {

                   }
               }).attach();


    }
}