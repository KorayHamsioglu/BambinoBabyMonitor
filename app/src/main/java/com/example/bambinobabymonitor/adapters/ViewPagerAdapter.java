package com.example.bambinobabymonitor.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.bambinobabymonitor.fragments.getstarted1;
import com.example.bambinobabymonitor.fragments.getstarted2;
import com.example.bambinobabymonitor.fragments.getstarted3;
import com.example.bambinobabymonitor.fragments.getstarted4;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                return new getstarted1();
            case 1:
                return new getstarted2();
            case 2:
                return new getstarted3();
            default:
                return new getstarted4();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
