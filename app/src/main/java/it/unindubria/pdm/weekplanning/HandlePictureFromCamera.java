package it.unindubria.pdm.weekplanning;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HandlePictureFromCamera {

    // CONSTANTS
    private static final int CAMERA = 10;
    private static final int READ_WRITE_FROM_STORAGE = 5;
    private static final String BASE_DIRECTORY_PATH = "/WeekPlanning";
    private static final String EXTENSION_IMAGES = ".jpg";

    // class' variables
    private String absolutePath = "";

    // Helper & others
    Helper helper = new Helper();

    public HandlePictureFromCamera() {
        // absolute path of the internal storage
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

    // get image file of the picture taken by the camera
    private File getImageFile(String uid, String date, String category) {
        String file = absolutePath + BASE_DIRECTORY_PATH + "/" + uid + "/" + date + "/" + category + "/" + uid + "_" + date + EXTENSION_IMAGES;
        return new File(file);
    }

    // get directory based on the parameters passed
    private File getDirectory(String uid, String date, String category) {
        if(uid == null && date == null && category == null) {
            return new File(absolutePath + BASE_DIRECTORY_PATH);
        } else if(date == null && category == null) {
            return new File(absolutePath + BASE_DIRECTORY_PATH + "/" + uid);
        } else if(category == null) {
            return new File(absolutePath + BASE_DIRECTORY_PATH + "/" + uid + "/" + date);
        } else {
            return new File(absolutePath + BASE_DIRECTORY_PATH + "/" + uid + "/" + date + "/" + category);
        }
    }

    public Intent takePicture(
        String uid,
        String date,
        String category,
        Context context,
        Activity activity
    ) {
        // check for permissions
        if(hasAppPermissions(context, Manifest.permission.CAMERA) && hasAppPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // create the directories structure if there isn't one
            helper.createDirectoryStructure(uid, date, category);
            // get the file which the picture taken will be stored in
            File newfile = getImageFile(uid, date, category);

            try {
                newfile.createNewFile();

                // create a provider
                Uri photoUri = FileProvider.getUriForFile(
                        context,
                        "com.example.android.fileprovider",
                        newfile
                );
                // create intent to take the picture by the camera
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                return takePhotoIntent;
            }
            catch (IOException e) {
                helper.displayWithToast(context, context.getString(R.string.error_file_not_created));
            }

            return null;
        } else {
            requestPermissions(activity, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, CAMERA);

            return null;
        }
    }

    private void deleteDirectoryOrFile(File dirToDelete) {
        dirToDelete.delete();
    }

    private void clearEmptyDirectories(String uid, String date, String category) {
        // directory of the section that contained the image (breakfast, lunch, dinner)
        File currentDirectory = getDirectory(uid, date, category);

        // if it's empty it needs to be removed
        if(currentDirectory.exists() && currentDirectory.list().length == 0) {
            deleteDirectoryOrFile(currentDirectory);
        }

        // directory of a specific date which can contain the categories' folders
        currentDirectory = getDirectory(uid, date, null);

        // if it's empty it needs to be removed
        if(currentDirectory.exists() && currentDirectory.list().length == 0) {
            deleteDirectoryOrFile(currentDirectory);
        }

        // directory of the current user which contains all his meals' pictures
        currentDirectory = getDirectory(uid, null, null);

        // if it's empty it needs to be removed
        if(currentDirectory.exists() && currentDirectory.list().length == 0) {
            deleteDirectoryOrFile(currentDirectory);
        }
    }

    // transfer the content from the file containing the picture taken by the camera, to the ImageView
    public void setPreviewImage(
        String uid,
        String date,
        String category,
        ImageView previewImage,
        Context context,
        Activity activity
    ) {
        // check for permission to read external storage (having already write we used this)
        if(hasAppPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            File imgFile = getImageFile(uid, date, category);

            if(imgFile.exists()) {
                if(imgFile.length() > 0) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                    previewImage.setMaxHeight(680);
                    previewImage.setImageBitmap(myBitmap);
                } else {
                    imgFile.delete();
                }
            }
        } else {
            requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, READ_WRITE_FROM_STORAGE);
        }
    }

    private void deleteImage(File fileToDelete, String uid, String date, String category, ImageView previewImage) {
        // delete image
        deleteDirectoryOrFile(fileToDelete);
        // delete parents empty directories
        clearEmptyDirectories(uid, date, category);
        // clear the data contained inside the ImageView UI element
        // (to show the user that the image was deleted)
        previewImage.setImageBitmap(null);
        previewImage.setMaxHeight(0);
    }

    /*
    *   This method handle the deletion of an image
    *
    *   @param  withAuthorization   Specify if the permission from the user is needed
    *   @param  uid                 An unique identifier for the user
    *   @param  date                A string representing the date when the meal will be consumed
    *   @param  category            The category of the meal ("breakfast", "lunch", "dinner")
    *   @param  previewImage        An object representing the image element on the UI
    *   @param  context             The Context to access some methods
    **/
    public void handleDeleteImage(boolean withAuthorization, final String uid, final String date, final String category, final ImageView previewImage, Context context)  {
        final File fileToDelete = getImageFile(uid, date, category);

        if(fileToDelete.exists()) {
            if(withAuthorization) {
                new AlertDialog
                    .Builder(context)
                    .setTitle(context.getString(R.string.warning_delete_image_title))
                    .setMessage(context.getString(R.string.warning_delete_image_message))
                    .setPositiveButton(context.getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteImage(fileToDelete, uid, date, category, previewImage);
                        }
                    })
                    .setNegativeButton(context.getString(R.string.button_no), null)
                    .show();
            } else {
                // delete the image without the authorization from the user
                deleteImage(fileToDelete, uid, date, category, previewImage);
            }
        }
    }
}
