package com.freddieptf.meh.imagecompressor.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by freddieptf on 01/08/16.
 */
public class VPagerAdapter extends FragmentPagerAdapter {

    ArrayList<Fragment> fragments;

    public VPagerAdapter(FragmentManager fm) {
        super(fm);
        fragments = new ArrayList<>();
    }

    public void setFrags(ArrayList<Fragment> frags){
        this.fragments = frags;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "Scaling";
        else if (position == 1) return "Conversion";
        return "";
    }
}
