package it.unindubria.pdm.weekplanning;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TimePicker;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class AddBreakfast extends AppCompatActivity implements View.OnClickListener {
    // CONSTANTS
    private static final int CAMERA = 10;
    private static final int READ_WRITE_FROM_STORAGE = 5;
    private static final int TAKE_PHOTO_CODE = 11;
    private static final int REQUEST_CODE_GOOGLE_CALENDAR_PERMISSION = 222;

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;
    // Google Calendar API
    private com.google.api.services.calendar.Calendar service;
    private String weekPlanningCalendarId = "";

    // class' variables
    private ArrayList<Food> listFoodItems;
    private ArrayAdapter<Food> adapter;
    private ListView listView;
    private String startTime = null;
    private String endTime = null;

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private Button timePickerStartButton;
    private Button timePickerEndButton;
    private Button takePicture;
    private Button saveButton;
    private ImageView previewImage;
    private String dateSelected;

    // Helpers & Others
    private Helper helper = new Helper();
    private HandleAddMeals handleAddMeals = new HandleAddMeals();
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

        service = GoogleCalendarHelper.getCalendarBuilderInstance(
                AddBreakfast.this,
                mFirebaseUser.getEmail()
        );

        // connect elements to UI
        editTextFood = findViewById(R.id.insert_food);
        addFoodItemButton = findViewById(R.id.add_item_meal);
        timePickerStartButton = findViewById(R.id.time_picker_start);
        timePickerEndButton = findViewById(R.id.time_picker_end);
        takePicture = findViewById(R.id.take_picture_button);
        saveButton = findViewById(R.id.finish_button);
        previewImage = findViewById(R.id.preview_image_meal);
        listView = findViewById(R.id.list_food_items_meal);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        timePickerStartButton.setOnClickListener(this);
        timePickerEndButton.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        previewImage.setOnClickListener(this);
        handleRemoveListViewItem();

        // getting data from intent
        Intent mainActivityIntent = getIntent();
        // saving the date choosen by the user already formatted
        dateSelected = mainActivityIntent.getStringExtra("dateString");
        weekPlanningCalendarId = mainActivityIntent.getStringExtra("calendarId");

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(AddBreakfast.this);
        localDB.openWrite();

        handleAddMeals.setPreviewImage(uid, dateSelected, "breakfast", previewImage, AddBreakfast.this, AddBreakfast.this);

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
            case R.id.add_item_meal:
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
            case R.id.preview_image_meal:
                handleAddMeals.deleteImage(uid, dateSelected, "breakfast", previewImage, AddBreakfast.this);
                break;
            case R.id.time_picker_start:
                setTimeSelected(true);
                break;
            case R.id.time_picker_end:
                setTimeSelected(false);
                break;
            case R.id.take_picture_button:
                takePictureFromCamera();
                break;
            case R.id.finish_button:
                finishActivityAndGoBack();
        }
    }

    private void insertMealGoogleCalendar() {
        final String allFoodItemsString = helper.getStringListBreakfastItem(listFoodItems);

        // TODO: YOU NEED TO TAKE TRAKE OF TIME_START AND TIME_END IN THE DB

        if(startTime != null && endTime != null && helper.isThereAtLeastACharacter(allFoodItemsString))
        new Thread(new Runnable() {
            public void run() {
                try {
                    GoogleCalendarHelper.createNewEvent(
                        service,
                        weekPlanningCalendarId,
                        getString(R.string.section_meal_breakfast),
                        allFoodItemsString,
                        "2020-06-19",
                        startTime + ":00",
                        endTime + ":00"
                    );
                } catch(UserRecoverableAuthIOException exc) {
                    AddBreakfast.this.startActivityForResult(
                            exc.getIntent(),
                            AddBreakfast.REQUEST_CODE_GOOGLE_CALENDAR_PERMISSION
                    );
                } catch (IOException exc) {
                    Log.e("CALENDAR INFO", "ERROR CALENDAR READING ID", exc);
                }
            }
        }).start();
    }

    private void setTimeSelected(final boolean isStartTime) {
        int initialHours, initialMinutes;

        Calendar calendarUtil = Calendar.getInstance();

        initialHours = calendarUtil.get(Calendar.HOUR_OF_DAY);
        initialMinutes = calendarUtil.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(AddBreakfast.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String timeSelected = (hourOfDay < 10 ? "0" : "") + hourOfDay + ":" + (minute < 10 ? "0" : "") + minute;
                if(isStartTime) {
                    startTime = timeSelected;
                    timePickerStartButton.setText(startTime);
                } else {
                    endTime = timeSelected;
                    timePickerEndButton.setText(endTime);
                }
            }
        }, initialHours, initialMinutes, true);

        timePickerDialog.show();
    }

    private void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();

        if(!nameFoodItem.isEmpty() && helper.isThereAtLeastACharacter(nameFoodItem)) {
            Food food = new Food(nameFoodItem, dateSelected, "breakfast", uid);

            listFoodItems.add(0, food);
            adapter.notifyDataSetChanged();

            saveInfoToDB(food);

            editTextFood.setText("");
        } else {
            helper.displayWithToast(AddBreakfast.this, R.string.insert_empty_item);
        }
    }

    private void takePictureFromCamera() {
        Intent cameraIntent = handleAddMeals.takePicture(uid, dateSelected, "breakfast", AddBreakfast.this, AddBreakfast.this);

        if(cameraIntent != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
        }
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
        if (requestCode == CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                helper.displayWithToast(AddBreakfast.this, "Now you can take pictures!");
            } else {
                helper.displayWithToast(AddBreakfast.this, "Cannot to take or save pictures");
            }
        } else if (requestCode == READ_WRITE_FROM_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleAddMeals.setPreviewImage(uid, dateSelected, "breakfast", previewImage, AddBreakfast.this, AddBreakfast.this);
                helper.displayWithToast(AddBreakfast.this, "Take your picture again, please.");
            } else {
                helper.displayWithToast(AddBreakfast.this, "Cannot to take or save pictures");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            handleAddMeals.setPreviewImage(uid, dateSelected, "breakfast", previewImage, AddBreakfast.this, AddBreakfast.this);
        } else if(requestCode == REQUEST_CODE_GOOGLE_CALENDAR_PERMISSION && resultCode == RESULT_OK) {
            insertMealGoogleCalendar();
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
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }

    private void finishActivityAndGoBack() {
        insertMealGoogleCalendar();
        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }

    @Override
    public void onBackPressed() {
        finishActivityAndGoBack();
    }
}
