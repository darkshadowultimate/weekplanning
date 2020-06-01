package it.unindubria.pdm.weekplanning;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HandleAddMeals {

    // CONSTANTS
    private static final int CAMERA = 10;
    private static final int READ_WRITE_FROM_STORAGE = 5;
    private static final String BASE_DIRECTORY_PATH = "/WeekPlanning";
    private static final String EXTENSION_IMAGES = ".jpg";

    // class' variables
    private String absolutePath = "";

    // Helper & others
    Helper helper = new Helper();

    public HandleAddMeals() {
        absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public boolean hasAppPermissions(Context context, String typePermission) {
        return ContextCompat
                .checkSelfPermission(
                    context,
                    typePermission
                ) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissions(Activity activity, String[] permissions, int codePermission) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            codePermission
        );
    }

    public Intent takePicture(
        String uid,
        String date,
        String category,
        Context context,
        Activity activity
    ) {
        if(hasAppPermissions(context, Manifest.permission.CAMERA) && hasAppPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            helper.createDirectoryStructure(uid, date, category);

            String file = absolutePath + BASE_DIRECTORY_PATH + "/" + uid + "/" + date + "/" + category + "/" + uid + "_" + date + EXTENSION_IMAGES;
            File newfile = new File(file);

            try {
                newfile.createNewFile();

                Uri photoUri = FileProvider.getUriForFile(
                        context,
                        "com.example.android.fileprovider",
                        newfile
                );

                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                return takePhotoIntent;
            }
            catch (IOException e) {
                helper.displayWithToast(context, "File not created");
            }

            return null;
        } else {
            requestPermissions(activity, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, CAMERA);

            return null;
        }
    }

    public void setPreviewImage(
        String uid,
        String date,
        String category,
        ImageView previewImage,
        Context context,
        Activity activity
    ) {
        if(hasAppPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            String filePath = absolutePath + BASE_DIRECTORY_PATH + "/" + uid + "/" + date + "/" + category + "/" + uid + "_" + date + EXTENSION_IMAGES;
            File imgFile = new File(filePath);

            if(imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                previewImage.setImageBitmap(myBitmap);
            }
        } else {
            requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, READ_WRITE_FROM_STORAGE);
        }
    }
}
