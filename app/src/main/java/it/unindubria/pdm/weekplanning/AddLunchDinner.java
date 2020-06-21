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
import android.widget.Spinner;
import android.widget.TimePicker;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddLunchDinner extends AppCompatActivity implements View.OnClickListener {
    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;
    // Google Calendar API
    // service => used to authenticate all requests to Google APIs with OAuth2
    private com.google.api.services.calendar.Calendar service;
    private GoogleCalendarEvent googleCalendarEvent = null;
    private String weekPlanningCalendarId = "";
    private String idGoogleCalendarEvent = "";

    // class' variables
    private ArrayList<Food> listFoodItemsLunchDinner;
    private ArrayList<Food> listFoodItemsNew;
    private ArrayList<Food> listFoodItemsToDelete;
    private ArrayAdapter<Food> adapterLunchDinner;
    private ListView listViewLunchDinner;
    private String dateSelected;
    private String lunchOrDinner;
    private String summaryGoogleCalendarEvent;
    Map<String, Integer> prioritySubCategories;
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

        service = GoogleCalendarHelper.getCalendarBuilderInstance(
                AddLunchDinner.this,
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
        listViewLunchDinner = findViewById(R.id.list_food_items_meal);
        dropdown_subcategories = findViewById(R.id.dropdown_subcategory);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        timePickerStartButton.setOnClickListener(this);
        timePickerEndButton.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        previewImage.setOnClickListener(this);
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
        lunchOrDinner = mainActivityIntent.getStringExtra("lunchOrDinner");
        weekPlanningCalendarId = mainActivityIntent.getStringExtra("calendarId");

        summaryGoogleCalendarEvent = getString(
            getResources().getIdentifier(
                "section_meal_" + lunchOrDinner,
                "string",
                getPackageName()
            )
        );

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(AddLunchDinner.this);
        localDB.openWrite();

        // loading values of startTime and endTime from localDB
        googleCalendarEvent = localDB.getGoogleCalendarEvent(dateSelected, lunchOrDinner);

        if(googleCalendarEvent != null) {
            Log.e("CHECK CLASS", "googleCalendarEvent IS NOT NULL");

            startTime = googleCalendarEvent.getTimeStart();
            endTime = googleCalendarEvent.getTimeEnd();
            idGoogleCalendarEvent = googleCalendarEvent.getId();

            Log.e("CHECK VARIABLES TIME", startTime + "  ---  " + endTime);

            timePickerStartButton.setText(startTime);
            timePickerEndButton.setText(endTime);
        } else {
            Log.e("CHECK CLASS", "googleCalendarEvent IS ABSOLUTELY NULL");
        }

        handleAddMeals.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);

        // setting up listview and adapter
        listFoodItemsLunchDinner = new ArrayList<Food>();
        listFoodItemsNew = new ArrayList<Food>();
        listFoodItemsToDelete = new ArrayList<Food>();
        adapterLunchDinner = new ArrayAdapter<Food>(AddLunchDinner.this, android.R.layout.simple_list_item_1, listFoodItemsLunchDinner);
        listViewLunchDinner.setAdapter(adapterLunchDinner);

        // getting data from local DB SQLite
        synchronizeListFoodItemsWithLocalDB();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.add_item_meal:
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
            case R.id.preview_image_meal:
                handleAddMeals.handleDeleteImage(true, uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this);
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
                break;
        }
    }

    private String getSubcategoryStringValueForDB(String valueSelected) {
        for(int i = 0; i < Helper.SUBCATEGORIES_VOICES_DB.length; i++) {
            if(dropdown_subcategories.getItemAtPosition(i).toString().equals(valueSelected)) {
                return Helper.SUBCATEGORIES_VOICES_DB[i];
            }
        }
        return null;
    }

    private void setTimeSelected(final boolean isStartTime) {
        int initialHours, initialMinutes;

        if(startTime != null && endTime != null) {
            if(isStartTime) {
                initialHours = helper.getHoursFromString(startTime);
                initialMinutes = helper.getMinutesFromString(startTime);
            } else {
                initialHours = helper.getHoursFromString(endTime);
                initialMinutes = helper.getMinutesFromString(endTime);
            }
        } else {
            Calendar calendarUtil = Calendar.getInstance();

            initialHours = calendarUtil.get(Calendar.HOUR_OF_DAY);
            initialMinutes = calendarUtil.get(Calendar.MINUTE);
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(AddLunchDinner.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String timeSelected = helper.getStringTime(hourOfDay, minute);
                if(isStartTime) {
                    startTime = timeSelected;
                    timePickerStartButton.setText(startTime);
                    Log.e("START TIME VALUE =====> ", startTime);
                } else {
                    endTime = timeSelected;
                    timePickerEndButton.setText(endTime);
                    Log.e("END TIME VALUE =====> ", endTime);
                }
            }
        }, initialHours, initialMinutes, true);

        timePickerDialog.show();
    }

    private void takePictureFromCamera() {
        Intent cameraIntent = handleAddMeals.takePicture(uid, dateSelected, lunchOrDinner, AddLunchDinner.this, AddLunchDinner.this);

        if(cameraIntent != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, helper.getTakePictureCodeStartActivity(AddLunchDinner.this));
        }
    }

    private void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();
        String subcategory = getSubcategoryStringValueForDB(dropdown_subcategories.getSelectedItem().toString());

        if(subcategory == null) return;

        Food food = new Food(nameFoodItem, dateSelected, lunchOrDinner, subcategory, uid);

        food.setSubcategoryTranslation(getTranslationSubcategory(food.getSubcategory()));

        int positionToInsertItem = getPositionToInsertItem(food);

        Log.e("POSITION TO INSERT THE ELEMENT =====> ", String.valueOf(positionToInsertItem));

        listFoodItemsNew.add(food);
        listFoodItemsLunchDinner.add(positionToInsertItem, food);

        adapterLunchDinner.notifyDataSetChanged();

        // The user could add more items at the time.
        // Clearing the focus and hiding the keyboard would be bad UX.
        editTextFood.setText("");
    }

    private int getPositionToInsertItem(Food foodItem) {
        int priorityValArg = prioritySubCategories.get(foodItem.getSubcategory());
        int counter;

        for(counter = 0; counter < listFoodItemsLunchDinner.size(); counter++) {
            int priorityValItem = prioritySubCategories.get(listFoodItemsLunchDinner.get(counter).getSubcategory());

            if(priorityValArg < priorityValItem) {
                return counter;
            }
        }
        return counter;
    }

    private String getTranslationSubcategory(String keyword) {
        int idString = getResources().getIdentifier(keyword, "string", getPackageName());
        return getString(idString);
    }

    private void synchronizeListFoodItemsWithLocalDB() {
        ArrayList<Food> loadedFoodItems = localDB
                .getAllFoodItemsSection(uid, dateSelected, lunchOrDinner);

        sortListFoodItems(loadedFoodItems);
    }

    private void sortListFoodItems(ArrayList<Food> listToOrder) {
        for (String valueCategory : Helper.SUBCATEGORIES_VOICES_DB) {
            for(Food item: listToOrder) {
                if(item.getSubcategory().equals(valueCategory)) {
                    item.setSubcategoryTranslation(getTranslationSubcategory(item.getSubcategory()));
                    listFoodItemsLunchDinner.add(item);
                }
            }
        }

        adapterLunchDinner.notifyDataSetChanged();
        return;
    }

    private void insertUpdateMealGoogleCalendar() {
        final String allFoodItemsStringCalendarEvent = Helper.getStringListLunchDinnerItemsForDB(listFoodItemsLunchDinner, lunchOrDinner, AddLunchDinner.this);

        if(startTime != null && endTime != null && listFoodItemsLunchDinner.size() > 0) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        updateLocalDB();
                        if(googleCalendarEvent != null) {
                            if(
                                listFoodItemsNew.size() > 0 ||
                                listFoodItemsToDelete.size() > 0 ||
                                !googleCalendarEvent.getTimeStart().equals(startTime) ||
                                !googleCalendarEvent.getTimeEnd().equals(endTime)
                            ) {
                                Log.e("CHECK FOR UPDATE EVENT", "UPDATE EVENT");

                                GoogleCalendarHelper.updateCalendarEvent(
                                        service,
                                        weekPlanningCalendarId,
                                        idGoogleCalendarEvent,
                                        dateSelected,
                                        startTime,
                                        endTime,
                                        summaryGoogleCalendarEvent,
                                        allFoodItemsStringCalendarEvent
                                );

                                localDB.updateGoogleCalendarEvent(new GoogleCalendarEvent(
                                        idGoogleCalendarEvent,
                                        startTime,
                                        endTime,
                                        "",
                                        dateSelected,
                                        lunchOrDinner
                                ));
                            } else {
                                Log.e("CHECK FOR UPDATE EVENT", "NOT UPDATED!!!!!");
                            }
                        } else {
                            Log.e("CHECK FOR INSERT EVENT", "INSERT EVENT");

                            idGoogleCalendarEvent = GoogleCalendarHelper.createNewEvent(
                                    service,
                                    weekPlanningCalendarId,
                                    summaryGoogleCalendarEvent,
                                    allFoodItemsStringCalendarEvent,
                                    dateSelected,
                                    startTime,
                                    endTime
                            );

                            localDB.insertGoogleCalendarEvent(new GoogleCalendarEvent(
                                    idGoogleCalendarEvent,
                                    startTime,
                                    endTime,
                                    "",
                                    dateSelected,
                                    lunchOrDinner
                            ));
                        }
                    } catch(UserRecoverableAuthIOException exc) {
                        AddLunchDinner.this.startActivityForResult(
                                exc.getIntent(),
                                helper.getGoogleCalendarCodeStartActivity(AddLunchDinner.this)
                        );
                    } catch (IOException exc) {
                        Log.e("CALENDAR INFO", "ERROR CALENDAR READING ID", exc);
                    }
                }
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == helper.getCameraPermissionCode(AddLunchDinner.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                helper.displayWithToast(AddLunchDinner.this, "Now you can take pictures!");
            } else {
                helper.displayWithToast(AddLunchDinner.this, "Cannot to take or save pictures");
            }
        } else if (requestCode == helper.getStoragePermissionCode(AddLunchDinner.this)) {
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

        if (requestCode == helper.getTakePictureCodeStartActivity(AddLunchDinner.this) && resultCode == RESULT_OK) {
            handleAddMeals.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);
        }
    }

    private void handleRemoveListViewItem() {
        listViewLunchDinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            new AlertDialog
                .Builder(AddLunchDinner.this)
                .setTitle(getString(R.string.warning_remove_list_item_element_message))
                .setMessage(
                    listFoodItemsLunchDinner.size() == 1
                        ? getString(R.string.warning_remove_list_item_element_last)
                        : getString(R.string.warning_remove_list_item_element_message)
                )
                .setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    Food item = listFoodItemsLunchDinner.get(position);

                    if(!listFoodItemsNew.contains(item)) {
                        listFoodItemsToDelete.add(item);
                    } else {
                        listFoodItemsNew.remove(item);
                    }

                    listFoodItemsLunchDinner.remove(position);
                    adapterLunchDinner.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(getString(R.string.button_no), null)
                .show();
            }
        });
    }

    private void updateLocalDB() {
        Helper.addAllFoodItemsToDBWhichWereAdded(localDB, listFoodItemsNew);
        Helper.deleteAllFoodItemsFromDBWhichWereRemoved(localDB, listFoodItemsToDelete);
    }

    private void finishActivityAndGoBack() {
        Log.e("CONDITION START_TIME & END_TIME =======> ", String.valueOf(startTime != null && endTime != null));
        // startTime and endTime must have a value
        if(startTime != null && endTime != null) {
            Log.e("START_TIME AND END_TIME HAVE VALUES", startTime + " ---- " + endTime);
            // startTime must be less than endTime
            if(helper.isEndTimeBiggerThanStartTime(startTime, endTime)) {
                // if there are no meal's items,
                // than delete the google calendar's event and the meal's picture (if they exist)
                // otherwise update the SQLite DB and the google calendar's event
                if(listFoodItemsLunchDinner.size() == 0) {
                    Log.e("deleteGoogleCalendarEvent", "CALL FUNCTION TO DELETE");
                    updateLocalDB();
                    // delete the image
                    handleAddMeals.handleDeleteImage(false, uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this);
                    // delete the google calendar's event
                    Helper.deleteGoogleCalendarEvent(
                            service,
                            localDB,
                            googleCalendarEvent,
                            weekPlanningCalendarId,
                            idGoogleCalendarEvent,
                            dateSelected,
                            lunchOrDinner
                    );
                } else {
                    Log.e("insertUpdateMealGoogleCalendar", "CALL FUNCTION TO INSERT OR UPDATE");
                    // insert or update a google calendar's event
                    insertUpdateMealGoogleCalendar();
                }
                // set the result of the activityForResult and terminate the activity
                setResult(Activity.RESULT_OK, new Intent());
                finish();
            } else {
                Log.e("ERROR START_TIME > END_TIME =======> ", startTime + " ---- " + endTime);
                // warn the user than the startTime must be less than endTime
                helper.displayWithDialog(
                        AddLunchDinner.this,
                        getString(R.string.error_timestart_less_than_timeend_title),
                        getString(R.string.error_timestart_less_than_timeend_message)
                );
            }
        } else if(listFoodItemsLunchDinner.size() > 0) {
            Log.e(
            "ERROR START_TIME OR/AND END_TIME HAVE NO VALUES =======> ",
            (startTime == null ? "null" : startTime) + " ---- " + (endTime == null ? "null" : endTime)
            );
            // if there are meal's items and the startTime or the endTime is not set,
            // display an error message
            helper.displayWithDialog(
                    AddLunchDinner.this,
                    getString(R.string.error_time_not_selected_title),
                    getString(R.string.error_time_not_selected_message)
            );
        } else {
            // if there are no meal's items and the time is not set,
            // than terminate the activity
            setResult(Activity.RESULT_OK, new Intent());
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }
}
