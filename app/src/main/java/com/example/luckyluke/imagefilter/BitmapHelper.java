package com.example.luckyluke.imagefilter;

import android.graphics.Bitmap;

public class BitmapHelper {
    private Bitmap mBitmap = null;
    private static final BitmapHelper instance = new BitmapHelper();

    private BitmapHelper() {
    }

    public static BitmapHelper getInstance() {
        return instance;
    }

    public final Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }
}
