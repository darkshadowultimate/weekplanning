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
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddLunchDinner extends AppCompatActivity implements View.OnClickListener {

    // CONSTANTS
    private final String[] SUBCATEGORIES_VOICES_DB = { "before", "first", "second", "after" };
    private static final int CAMERA = 10;
    private static final int READ_WRITE_FROM_STORAGE = 5;
    private static final int TAKE_PHOTO_CODE = 11;

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;

    // class' variables
    private ArrayList<Food> listFoodItemsLunchDinner;
    private ArrayAdapter<Food> adapterLunchDinner;
    private ListView listViewLunchDinner;
    private String dateSelected;
    private String lunchOrDinner;
    Map<String, Integer> prioritySubCategories;

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private Button takePicture;
    private Button saveButton;
    private ImageView previewImage;
    private Spinner dropdown_subcategories;


    // Helpers & Others
    private Helper helper = new Helper();
    private HandleAddMeals handleAddMeals = new HandleAddMeals();
    private DBAdapter localDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lunch_dinner);

        // setting up Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser == null) {
            startActivity(helper.changeActivity(AddLunchDinner.this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
        }

        // connect elements to UI
        editTextFood = findViewById(R.id.dinner_lunch_insert_food);
        addFoodItemButton = findViewById(R.id.add_item_dinner_lunch);
        takePicture = findViewById(R.id.take_picture_button);
        saveButton = findViewById(R.id.dinner_lunch_finish_button);
        previewImage = findViewById(R.id.preview_image_meal);
        listViewLunchDinner = findViewById(R.id.listview_dinner_lunch);
        dropdown_subcategories = findViewById(R.id.dropdown_subcategory);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        handleRemoveListViewItem();

        // setting variables' values
        prioritySubCategories = new HashMap<String, Integer>();
        prioritySubCategories.put("before", new Integer(0));
        prioritySubCategories.put("first", new Integer(1));
        prioritySubCategories.put("second", new Integer(2));
        prioritySubCategories.put("after", new Integer(3));

        // getting data from intent
        Intent mainActivityIntent = getIntent();
        // saving the date choosen by the user already formatted
        dateSelected = mainActivityIntent.getStringExtra("dateString");
        lunchOrDinner = mainActivityIntent.getStringExtra("lunchOrLunch");

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(AddLunchDinner.this);
        localDB.openWrite();

        handleAddMeals.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);

        // setting up listview and adapter
        listFoodItemsLunchDinner = new ArrayList<Food>();
        adapterLunchDinner = new ArrayAdapter<Food>(AddLunchDinner.this, android.R.layout.simple_list_item_1, listFoodItemsLunchDinner);
        listViewLunchDinner.setAdapter(adapterLunchDinner);

        // getting data from local DB SQLite
        synchronizeListFoodItemsWithLocalDB();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.add_item_dinner_lunch:
                if(listFoodItemsLunchDinner.size() < 20) {
                    addFoodItem();
                } else {
                    helper.displayWithDialog(
                        AddLunchDinner.this,
                        R.string.no_more_items_title,
                        R.string.no_more_items_message
                    );
                }
                break;
            case R.id.take_picture_button:
                takePictureFromCamera();
                break;
            case R.id.dinner_lunch_finish_button:
                finishActivityAndGoBack();
                break;
        }
    }

    private String getSubcategoryStringValueForDB(String valueSelected) {
        for(int i = 0; i < SUBCATEGORIES_VOICES_DB.length; i++) {
            if(dropdown_subcategories.getItemAtPosition(i).toString().equals(valueSelected)) {
                return SUBCATEGORIES_VOICES_DB[i];
            }
        }
        return null;
    }

    private void takePictureFromCamera() {
        Intent cameraIntent = handleAddMeals.takePicture(uid, dateSelected, lunchOrDinner, AddLunchDinner.this, AddLunchDinner.this);

        if(cameraIntent != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
        }
    }

    private void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();
        String subcategory = getSubcategoryStringValueForDB(dropdown_subcategories.getSelectedItem().toString());

        if(subcategory == null) return;

        Food food = new Food(nameFoodItem, dateSelected, lunchOrDinner, subcategory, uid);

        //insertItemToArrayList(food);
        listFoodItemsLunchDinner.add(getPositionToInsertItem(food), food);
        //sortListFoodItems(listFoodItemsLunchDinner);

        adapterLunchDinner.notifyDataSetChanged();

        saveInfoToDB(food);

        // The user could add more items at the time.
        // Clearing the focus and hiding the keyboard would be bad UX.
        editTextFood.setText("");
    }

    private int getPositionToInsertItem(Food foodItem) {
        int priorityValArg = prioritySubCategories.get(foodItem.getSubcategory());
        int counter;

        for(counter = 0; counter < listFoodItemsLunchDinner.size(); counter++) {
            int priorityValItem = prioritySubCategories
                    .get(listFoodItemsLunchDinner
                            .get(counter).getSubcategory());

            if(priorityValArg < priorityValItem) {
                return counter;
            }
        }
        return counter;
    }

    private void saveInfoToDB(Food item) {
        if(listFoodItemsLunchDinner.size() > 0) {
            // update local SQLite database
            long idItem = localDB.insert(item);

            item.setId(idItem);
        }
    }

    private void synchronizeListFoodItemsWithLocalDB() {
        ArrayList<Food> loadedFoodItems = localDB
                .getAllFoodItemsSection(uid, dateSelected, lunchOrDinner);

        sortListFoodItems(loadedFoodItems);
    }

    private void sortListFoodItems(ArrayList<Food> listToOrder) {
        for (String valueCategory : SUBCATEGORIES_VOICES_DB) {
            for(Food item: listToOrder) {
                if(item.getSubcategory().equals(valueCategory)) {
                    listFoodItemsLunchDinner.add(item);
                }
            }
        }

        adapterLunchDinner.notifyDataSetChanged();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                helper.displayWithToast(AddLunchDinner.this, "Now you can take pictures!");
            } else {
                helper.displayWithToast(AddLunchDinner.this, "Cannot to take or save pictures");
            }
        } else if (requestCode == READ_WRITE_FROM_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleAddMeals.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);
                helper.displayWithToast(AddLunchDinner.this, "Take your picture again, please.");
            } else {
                helper.displayWithToast(AddLunchDinner.this, "Cannot to take or save pictures");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            try {
                handleAddMeals.savePictureToStorage(data, uid, dateSelected, lunchOrDinner, AddLunchDinner.this);
                handleAddMeals.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRemoveListViewItem() {
        listViewLunchDinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog
                    .Builder(AddLunchDinner.this)
                    .setTitle("Remove element")
                    .setMessage("Do you really wanna remove this element?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Food item = listFoodItemsLunchDinner.get(position);

                            listFoodItemsLunchDinner.remove(position);
                            adapterLunchDinner.notifyDataSetChanged();

                            localDB.removeFoodItem(item.getId());
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
