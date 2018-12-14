package com.example.luckyluke.imagefilter;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.luckyluke.imagefilter.Adapter.ViewPagerAdapter;
import com.example.luckyluke.imagefilter.Interface.EditImageFragmentListener;
import com.example.luckyluke.imagefilter.Interface.FiltersListFragmentListener;
import com.example.luckyluke.imagefilter.utils.BitmapUtils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ProcessActivity extends AppCompatActivity
        implements FiltersListFragmentListener,
        EditImageFragmentListener {

    ImageView mImagePreview;
    ViewPager mFilterPreview;
    ViewPagerAdapter mAdapter;
    TabLayout mTabs;
    ImageView mHome, mSave, mSend;

    Bitmap mBitmap = BitmapHelper.getInstance().getBitmap();

    Bitmap originalImage;
    // to backup image_dark with filter applied
    Bitmap filteredImage;

    // the final image_dark after applying
    // brightness, saturation, contrast
    Bitmap finalImage;

    FiltersListFragment filtersListFragment;
    EditImageFragment editImageFragment;

    // modified image_dark values
    int brightnessFinal = 0;
    float saturationFinal = 1.0f;
    float contrastFinal = 1.0f;

    // load native image_dark filters library
    static {
        System.loadLibrary("NativeImageProcessor");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        initView();

        mHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToGallery();
            }
        });
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareImage();
            }
        });
    }

    private void initView() {
        mImagePreview = findViewById(R.id.iv_content_process_image_preview);
        mFilterPreview = findViewById(R.id.vp_activity_process_view_pager);
        mTabs = findViewById(R.id.tl_activity_process_tabs);

        mHome = findViewById(R.id.iv_activity_process_home);
        mSave = findViewById(R.id.iv_activity_process_save);
        mSend = findViewById(R.id.iv_activity_process_send);

        loadImage(mBitmap);

        setupViewPager(mFilterPreview);
        mTabs.setupWithViewPager(mFilterPreview);
    }

    private void loadImage(Bitmap bitmap) {
        // clear bitmap memory
        if (originalImage != null) {
            originalImage.recycle();
        }
        if (finalImage != null) {
            finalImage.recycle();
        }
        if (filteredImage != null) {
            filteredImage.recycle();
        }

        originalImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        filteredImage = originalImage.copy(Bitmap.Config.ARGB_8888, true);
        finalImage = originalImage.copy(Bitmap.Config.ARGB_8888, true);
        mImagePreview.setImageBitmap(originalImage);
    }

    @Override
    protected void onRestart() {
        originalImage = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        filtersListFragment = new FiltersListFragment(originalImage);
        mAdapter.notifyDataSetChanged();
        super.onRestart();
    }

    private void setupViewPager(ViewPager viewPager) {
        mAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // adding filter list fragment
        filtersListFragment = new FiltersListFragment(originalImage);
        filtersListFragment.setListener(this);

        // adding edit image_dark fragment
        editImageFragment = new EditImageFragment();
        editImageFragment.setListener(this);

        mAdapter.addFragment(filtersListFragment, getString(R.string.tab_filters));
        mAdapter.addFragment(editImageFragment, getString(R.string.tab_edit));

        viewPager.setAdapter(mAdapter);
    }

    @Override
    public void onBrightnessChanged(final int brightness) {
        brightnessFinal = brightness;
        Filter myFilter = new Filter();
        myFilter.addSubFilter(new BrightnessSubFilter(brightness));
        mImagePreview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888, true)));
    }

    @Override
    public void onSaturationChanged(final float saturation) {
        saturationFinal = saturation;
        Filter myFilter = new Filter();
        myFilter.addSubFilter(new SaturationSubfilter(saturation));
        mImagePreview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888, true)));
    }

    @Override
    public void onContrastChanged(final float contrast) {
        contrastFinal = contrast;
        Filter myFilter = new Filter();
        myFilter.addSubFilter(new ContrastSubFilter(contrast));
        mImagePreview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888, true)));
    }

    @Override
    public void onEditStarted() {

    }

    @Override
    public void onEditCompleted() {
        // once the editing is done i.e seekbar is drag is completed,
        // apply the values on to filtered image_dark
        final Bitmap bitmap = filteredImage.copy(Bitmap.Config.ARGB_8888, true);

        Filter myFilter = new Filter();
        myFilter.addSubFilter(new BrightnessSubFilter(brightnessFinal));
        myFilter.addSubFilter(new ContrastSubFilter(contrastFinal));
        myFilter.addSubFilter(new SaturationSubfilter(saturationFinal));
        finalImage = myFilter.processFilter(bitmap);
    }

    @Override
    public void onFilterSelected(Filter filter) {
        // reset image_dark controls
        resetControls();

        // applying the selected filter
        filteredImage = originalImage.copy(Bitmap.Config.ARGB_8888, true);
        // preview filtered image_dark
        mImagePreview.setImageBitmap(filter.processFilter(filteredImage));

        finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888, true);
    }

    /**
     * Resets image_dark edit controls to normal when new filter
     * is selected
     */
    private void resetControls() {
        if (editImageFragment != null) {
            editImageFragment.resetControls();
        }
        brightnessFinal = 0;
        saturationFinal = 1.0f;
        contrastFinal = 1.0f;
    }

    // Share image to other apps
    private void shareImage() {

        // save bitmap to cache directory
        try {

            File cachePath = new File(getApplicationContext().getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.jpeg"); // overwrites this image_dark every time
            finalImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Share image
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        final File photoFile = new File(getFilesDir(), "foo.jpg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
        startActivity(Intent.createChooser(shareIntent, "Share image using"));
//        File imagePath = new File(getApplicationContext().getCacheDir(), "images");
//        File newFile = new File(imagePath, "image.jpeg");
//        Uri contentUri = Uri.fromFile(newFile);
//
//        if (contentUri != null) {
//            Intent shareIntent = new Intent();
//            shareIntent.setAction(Intent.ACTION_SEND);
//            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
//            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
//            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
//            startActivity(Intent.createChooser(shareIntent, "Share image to"));
//        }
    }

    // Save image to gallery (***.JPEG)
    private void saveImageToGallery() {
        Dexter.withActivity(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            final String path = BitmapUtils.insertImage(getContentResolver(), finalImage, "IMG_"+ System.currentTimeMillis()+ "_profile.jpg", null);
                            if (!TextUtils.isEmpty(path)) {
                                Snackbar snackbar = Snackbar
                                        .make(mFilterPreview, "Image saved to gallery!", Snackbar.LENGTH_LONG)
                                        .setAction("OPEN", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                openImage(path);
                                            }
                                        });

                                snackbar.show();
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(mFilterPreview, "Unable to save image!", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Permissions are not granted!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    // opening image in default image viewer app
    private void openImage(String path) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), "image/*");
        startActivity(intent);
    }
}
