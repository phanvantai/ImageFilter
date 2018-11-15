package com.example.luckyluke.imagefilter;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.luckyluke.imagefilter.utils.BitmapUtils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_GALLERY_IMAGE = 11;
    private static final int REQUEST_TAKE_PHOTO = 12;
    private static final String AUTHORITY = "com.example.luckyluke.imagefilter";

    ImageView mCamera, mPhoto;

    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        mCamera = findViewById(R.id.iv_activity_main_camera);
        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        mPhoto = findViewById(R.id.iv_activity_main_image);
        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageFromGallery();
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void takePhoto() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                            // Ensure that there's a camera activity to handle the intent
                            if (mIntent.resolveActivity(getPackageManager()) != null) {
                                // Create the File where the photo should go
                                File photoFile = null;
                                try {
                                    photoFile = createImageFile();
                                } catch (IOException ex) {
                                    // Error occurred while creating the File
                                }
                                // Continue only if the File was successfully created
                                if (photoFile != null) {
                                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                            AUTHORITY,
                                            photoFile);
                                    mIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                    startActivityForResult(mIntent, REQUEST_TAKE_PHOTO);
                                }
                            }

                        }
                        else {
                            Toast.makeText(getApplicationContext(), "Permissions are not granted!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void openImageFromGallery() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, SELECT_GALLERY_IMAGE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_GALLERY_IMAGE) {
                Bitmap bitmapFromGallery = BitmapUtils.getBitmapFromGallery(this, data.getData(), 800, 800);

                Bitmap bitmap = bitmapFromGallery.copy(Bitmap.Config.ARGB_8888, true);
                BitmapHelper.getInstance().setBitmap(bitmap);
                bitmapFromGallery.recycle();
            }

            if (requestCode == REQUEST_TAKE_PHOTO) {
                Bitmap mBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
                //Bitmap mBitmap = BitmapUtils.getBitmapFromGallery(this, mImageUri, 800, 800);

                Bitmap bitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                BitmapHelper.getInstance().setBitmap(bitmap);
                mBitmap.recycle();
            }

            if (BitmapHelper.getInstance().getBitmap() != null) {
                Intent mIntent = new Intent(MainActivity.this, ProcessActivity.class);
                startActivity(mIntent);
            }
        }
        else {
            Toast.makeText(getApplicationContext(),
                    "You have not selected any images yet!",
                    Toast.LENGTH_LONG).show();
        }
    }
}
