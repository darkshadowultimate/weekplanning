package it.unindubria.pdm.weekplanning;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class AddBreakfast extends AppCompatActivity implements View.OnClickListener {

    // CONSTANTS
    private static final int TAKE_PHOTO_CODE = 10;

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;

    // class' variables
    private ArrayList<Food> listFoodItems;
    private ArrayAdapter<Food> adapter;
    private ListView listView;

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private Button takePicture;
    private Button saveButton;
    private ImageView previewImage;
    private String dateSelected;

    // Helpers & Others
    private Helper helper = new Helper();
    private DBAdapter localDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_breakfast);

        // setting up Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser == null) {
            startActivity(helper.changeActivity(AddBreakfast.this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
        }

        // connect elements to UI
        editTextFood = findViewById(R.id.breakfast_insert_food);
        addFoodItemButton = findViewById(R.id.add_item_breakfast);
        takePicture = findViewById(R.id.take_picture_button);
        saveButton = findViewById(R.id.breakfast_finish_button);
        previewImage = findViewById(R.id.preview_image_meal);
        listView = findViewById(R.id.breakfast_list_food_items);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        handleRemoveListViewItem();

        // getting data from intent
        Intent mainActivityIntent = getIntent();
        // saving the date choosen by the user already formatted
        dateSelected = mainActivityIntent.getStringExtra("dateString");

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(AddBreakfast.this);
        localDB.openWrite();

        helper.createNewDirectory("/WeekPlanning/" + uid + "/" + dateSelected);
        setPreviewImage();

        // setting up listview and adapter
        listFoodItems = new ArrayList<Food>();
        adapter = new ArrayAdapter<Food>(AddBreakfast.this, android.R.layout.simple_list_item_1, listFoodItems);
        listView.setAdapter(adapter);

        // getting data from local DB SQLite
        synchronizeListFoodItemsWithLocalDB();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.add_item_breakfast:
                if(listFoodItems.size() < 10) {
                    addFoodItem();
                } else {
                    helper.displayWithDialog(
                        AddBreakfast.this,
                        R.string.no_more_items_title,
                        R.string.no_more_items_message
                    );
                }
                break;
            case R.id.take_picture_button:
                handleTakePicture();
                break;
            case R.id.breakfast_finish_button:
                finishActivityAndGoBack();
        }
    }

    private void setPreviewImage() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WeekPlanning/" + uid + "/" + dateSelected + "/breakfast/" + uid + "_" + dateSelected + ".png";
        File imgFile = new File(filePath);

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            previewImage.setImageBitmap(myBitmap);
        }
    }

    private void handleTakePicture() {
        if (ContextCompat.checkSelfPermission(AddBreakfast.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                AddBreakfast.this,
                new String[] {
                    Manifest.permission.CAMERA
                },
                TAKE_PHOTO_CODE
            );
        } else {
            helper.createNewDirectory("/WeekPlanning/" + uid + "/" + dateSelected + "/breakfast");

            String file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WeekPlanning/" + uid + "/" + dateSelected + "/breakfast/" + uid + "_" + dateSelected + ".png";
            File newfile = new File(file);
            try {
                newfile.createNewFile();
            }
            catch (IOException e) {
                helper.displayWithToast(AddBreakfast.this, "File not created");
            }

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
            }
        }
    }

    private void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();
        Food food = new Food(nameFoodItem, dateSelected, "breakfast", uid);

        listFoodItems.add(0, food);
        adapter.notifyDataSetChanged();

        saveInfoToDB(food);

        // The user could add more items at the time.
        // Clearing the focus and hiding the keyboard would be bad UX.
        editTextFood.setText("");
    }

    private void saveInfoToDB(Food item) {
        if(listFoodItems.size() > 0) {
            // update local SQLite database
            long idItem = localDB.insert(item);

            item.setId(idItem);
        }
    }

    private void synchronizeListFoodItemsWithLocalDB() {
        ArrayList<Food> loadedFoodItems = localDB
            .getAllFoodItemsSection(uid, dateSelected, "breakfast");

        for(Food item: loadedFoodItems) {
            listFoodItems.add(0, item);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == TAKE_PHOTO_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleTakePicture();
            } else {
                helper.displayWithToast(AddBreakfast.this, "You won't be able to take pictures");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/WeekPlanning/" + uid + "/" + dateSelected + "/breakfast/" + uid + "_" + dateSelected + ".png");

            helper.displayWithToast(AddBreakfast.this, "THE IMAGE HAS BEEN TAKEN");
            try (FileOutputStream out = new FileOutputStream(file)) {
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance

                setPreviewImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRemoveListViewItem() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog
                    .Builder(AddBreakfast.this)
                    .setTitle("Remove element")
                    .setMessage("Do you really wanna remove this element?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Food item = listFoodItems.get(position);

                            listFoodItems.remove(position);
                            adapter.notifyDataSetChanged();

                            localDB.removeFoodItem(item.getId());

                            //TODO: REMOVE ITEM FROM FIREBASE TOO
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }

    private void finishActivityAndGoBack() {
        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }

    @Override
    public void onBackPressed() {
        finishActivityAndGoBack();
    }
}
