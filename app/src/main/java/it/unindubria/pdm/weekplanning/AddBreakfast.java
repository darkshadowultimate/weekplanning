package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.calendar.Calendar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;

public class AddBreakfast extends AppCompatActivity implements View.OnClickListener {
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
    private ArrayList<Food> listFoodItems;
    private ArrayList<Food> listFoodItemsNew;
    private ArrayList<Food> listFoodItemsToDelete;
    private ArrayAdapter<Food> adapter;
    private ListView listView;

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private Button timePickerStartButton;
    private Button timePickerEndButton;
    private Button takePicture;
    private Button deleteMealButton;
    private Button saveButton;
    private ImageView previewImage;
    private String dateSelected;

    // Helpers & Others
    private Helper helper = new Helper();
    private HandlePictureFromCamera handlePictureFromCamera = new HandlePictureFromCamera();
    // class to handle timeStart and timeEnd for the Google Calendar taken by the TimePicker
    private TimeEvent timeEvent;
    private DBAdapter localDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_breakfast);

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);

        // setting up Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        // check if the user is logged in
        if(mFirebaseUser == null) {
            // if not, start the activity LogIn.java
            startActivity(helper.changeActivity(AddBreakfast.this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
        }
        // setting Google Calendar Builder to authorize all the requests to Google Calendar with OAuth2
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
        deleteMealButton = findViewById(R.id.delete_button);
        saveButton = findViewById(R.id.finish_button);
        previewImage = findViewById(R.id.preview_image_meal);
        listView = findViewById(R.id.list_food_items_meal);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        timePickerStartButton.setOnClickListener(this);
        timePickerEndButton.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        deleteMealButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        previewImage.setOnClickListener(this);
        handleRemoveListViewItem();

        // getting data from intent (sent by MainActivity.java)
        Intent mainActivityIntent = getIntent();
        // saving the date choosen by the user already formatted (yyyy-mm-dd)
        dateSelected = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_dateString));
        weekPlanningCalendarId = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_calendarId));
        // change the title of the toolbar to => {date_selected_by_the_user} / Breakfast
        toolbar.setTitle(dateSelected + " / " + getString(R.string.section_meal_breakfast));

        // open connection to local SQLite database (write mode)
        localDB = DBAdapter.getInstance(AddBreakfast.this);
        localDB.openWrite();

        // loading (in particular), startTime and endTime values from local SQLite db
        googleCalendarEvent = localDB.getGoogleCalendarEvent(dateSelected, getString(R.string.constant_breakfast));
        // if an event has already been created for this meal in this day,
        // then set the timeStart and timeEnd,
        // otherwise initialize them by the default constructor of TimeEvent()
        if(googleCalendarEvent != null) {
            Log.e("CHECK CLASS", "googleCalendarEvent IS NOT NULL");

            timeEvent = new TimeEvent(
                googleCalendarEvent.getTimeStart(),
                googleCalendarEvent.getTimeEnd()
            );

            idGoogleCalendarEvent = googleCalendarEvent.getId();

            Log.e("CHECK VARIABLES TIME", timeEvent.getTimeStart() + "  ---  " + timeEvent.getTimeEnd());

            timePickerStartButton.setText(timeEvent.getTimeStart());
            timePickerEndButton.setText(timeEvent.getTimeEnd());
        } else {
            timeEvent = new TimeEvent();
            Log.e("CHECK CLASS", "googleCalendarEvent IS ABSOLUTELY NULL");
        }

        // load the image for this meal in this day (if it exists)
        handlePictureFromCamera.setPreviewImage(uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this, AddBreakfast.this);

        // setting up listview and adapter
        listFoodItems = new ArrayList<Food>();
        listFoodItemsNew = new ArrayList<Food>();
        listFoodItemsToDelete = new ArrayList<Food>();
        adapter = new ArrayAdapter<Food>(AddBreakfast.this, android.R.layout.simple_list_item_1, listFoodItems);
        listView.setAdapter(adapter);

        // getting data from local SQLite db
        synchronizeListFoodItemsWithLocalDB();
    }

    @Override
    public void onClick(View view) {
        // check if the device is online
        if(GoogleAPIHelper.isDeviceOnline(AddBreakfast.this)) {
            switch(view.getId()) {
                case R.id.add_item_meal:
                    // there can be max 10 items
                    if(listFoodItems.size() < 10) {
                        addFoodItem();
                    } else {
                        // there can be inserted max 10 items (we don't want the user eating to much!)
                        helper.displayWithDialog(
                            AddBreakfast.this,
                            R.string.no_more_items_title,
                            R.string.no_more_items_message
                        );
                    }
                    break;
                case R.id.preview_image_meal:
                    // if the user clicked the image's preview, handle the image deletion
                    handlePictureFromCamera.handleDeleteImage(true, uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this);
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
                case R.id.delete_button:
                    Helper.deleteMealCardImmediatly(
                        AddBreakfast.this,
                        AddBreakfast.this,
                        localDB,
                        service,
                        previewImage,
                        getString(R.string.constant_breakfast),
                        dateSelected,
                        uid,
                        weekPlanningCalendarId,
                        idGoogleCalendarEvent,
                        listFoodItems.size(),
                        listFoodItemsNew.size(),
                        listFoodItemsToDelete.size()
                    );
                    break;
                case R.id.finish_button:
                    finishActivityAndGoBack();
            }
        }
    }

    // handle the creation or the update of a new event in the user's Google Calendar
    private void insertUpdateMealGoogleCalendar() {
        // convert the food items of this meal from db into string
        final String allFoodItemsStringDB = helper.getStringListBreakfastItemForDB(listFoodItems);
        final String allFoodItemsStringCalendarEvent = helper.getStringListBreakfastItem(listFoodItems);
        // if the time was defined and if the allFoodItemsStringDB has content
        if(timeEvent.isTimeEventDefined() && helper.isThereAtLeastACharacter(allFoodItemsStringDB)) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if(googleCalendarEvent != null) {
                            ArrayList<String> foodItemsFromCalendarEventDB = helper.convertListBreakfastItemsDBToArrayList(allFoodItemsStringDB);

                            if(helper.areListItemsDifferent(foodItemsFromCalendarEventDB, listFoodItems)) {
                                Log.e("CHECK FOR UPDATE EVENT", "UPDATE EVENT");

                                GoogleCalendarHelper.updateCalendarEvent(
                                    service,
                                    weekPlanningCalendarId,
                                    idGoogleCalendarEvent,
                                    dateSelected,
                                    timeEvent.getTimeStart(),
                                    timeEvent.getTimeEnd(),
                                    getString(R.string.section_meal_breakfast),
                                    allFoodItemsStringCalendarEvent
                                );

                                localDB.updateGoogleCalendarEvent(new GoogleCalendarEvent(
                                    idGoogleCalendarEvent,
                                    timeEvent.getTimeStart(),
                                    timeEvent.getTimeEnd(),
                                    allFoodItemsStringDB,
                                    dateSelected,
                                    getString(R.string.constant_breakfast)
                                ));
                            } else {
                                Log.e("CHECK FOR UPDATE EVENT", "NOT UPDATED!!!!!");
                            }
                        } else {
                            Log.e("CHECK FOR INSERT EVENT", "INSERT EVENT");

                            idGoogleCalendarEvent = GoogleCalendarHelper.createNewEvent(
                                service,
                                weekPlanningCalendarId,
                                getString(R.string.section_meal_breakfast),
                                allFoodItemsStringCalendarEvent,
                                dateSelected,
                                timeEvent.getTimeStart(),
                                timeEvent.getTimeEnd()
                            );

                            localDB.insertGoogleCalendarEvent(new GoogleCalendarEvent(
                                    idGoogleCalendarEvent,
                                    timeEvent.getTimeStart(),
                                    timeEvent.getTimeEnd(),
                                    allFoodItemsStringDB,
                                    dateSelected,
                                    getString(R.string.constant_breakfast)
                            ));
                        }
                    } catch(UserRecoverableAuthIOException exc) {
                        AddBreakfast.this.startActivityForResult(
                                exc.getIntent(),
                                helper.getGoogleCalendarCodeStartActivity(AddBreakfast.this)
                        );
                    } catch (IOException exc) {
                        Log.e("CALENDAR INFO", "ERROR CALENDAR READING ID", exc);
                    }
                }
            }).start();
        }
    }

    // set the time by the TimePicker
    private void setTimeSelected(final boolean isStartTime) {
        helper.setTimeWithTimePicker(
            AddBreakfast.this,
            timeEvent,
            timePickerStartButton,
            timePickerEndButton,
            isStartTime
        );
    }

    // add new item to the listFoodItems ArrayList
    private void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();

        if(!nameFoodItem.isEmpty() && helper.isThereAtLeastACharacter(nameFoodItem)) {
            Food food = new Food(nameFoodItem, dateSelected, getString(R.string.constant_breakfast), uid);

            listFoodItemsNew.add(food);
            listFoodItems.add(food);
            adapter.notifyDataSetChanged();

            editTextFood.setText("");
        } else {
            helper.displayWithToast(AddBreakfast.this, R.string.insert_empty_item);
        }
    }

    private void takePictureFromCamera() {
        Intent cameraIntent = handlePictureFromCamera.takePicture(uid, dateSelected, getString(R.string.constant_breakfast), AddBreakfast.this, AddBreakfast.this);

        if(cameraIntent != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
            // open camera to take picture
            startActivityForResult(cameraIntent, helper.getTakePictureCodeStartActivity(AddBreakfast.this));
        }
    }

    // load data from database and update the listFoodItems ArrayList
    private void synchronizeListFoodItemsWithLocalDB() {
        ArrayList<Food> loadedFoodItems = localDB
            .getAllFoodItemsSection(uid, dateSelected, getString(R.string.constant_breakfast));

        for(Food item: loadedFoodItems) {
            listFoodItems.add(0, item);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // request permission for camera
        if (requestCode == helper.getCameraPermissionCode(AddBreakfast.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                helper.displayWithToast(AddBreakfast.this, getString(R.string.success_can_take_picture));
            } else {
                helper.displayWithToast(AddBreakfast.this, getString(R.string.error_cannot_take_picture));
            }
        // request permission for reading/writing on storage
        } else if (requestCode == helper.getStoragePermissionCode(AddBreakfast.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // load image's preview (if exists) from internal storage
                handlePictureFromCamera.setPreviewImage(uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this, AddBreakfast.this);
            } else {
                helper.displayWithToast(AddBreakfast.this, getString(R.string.error_cannot_save_picture));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // finished activity for taking picture by the camera
        if (requestCode == helper.getTakePictureCodeStartActivity(AddBreakfast.this) && resultCode == RESULT_OK) {
            handlePictureFromCamera.setPreviewImage(uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this, AddBreakfast.this);
        // finished activity for Google Calendar's permissions
        } else if(requestCode == helper.getGoogleCalendarCodeStartActivity(AddBreakfast.this) && resultCode == RESULT_OK) {
            insertUpdateMealGoogleCalendar();
        }
    }

    // handle mechanism to remove an element selected from the listView
    private void handleRemoveListViewItem() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            Helper.handleRemoveListViewItem(
                AddBreakfast.this,
                listFoodItems,
                listFoodItemsNew,
                listFoodItemsToDelete,
                adapter,
                position
            );
            }
        });
    }

    // check all cases in order to not create inconsistency and update SQLite local db and Google Calendar
    // (than terminate the activity)
    private void finishActivityAndGoBack() {
        // startTime and endTime must have a value
        if(timeEvent.isTimeEventDefined()) {
            // startTime must be less than endTime
            if(timeEvent.isTimeStartLessThanTimeEnd()) {
                if(listFoodItems.size() == 0 || timeEvent.isTimeEventEndInTheFuture(dateSelected)) {
                    Helper.addAllFoodItemsToDBWhichWereAdded(localDB, listFoodItemsNew);
                    Helper.deleteAllFoodItemsFromDBWhichWereRemoved(localDB, listFoodItemsToDelete);
                    // if there are no meal's items,
                    // than delete the google calendar's event and the meal's picture (if they exist)
                    // otherwise update the SQLite DB and the google calendar's event
                    if(listFoodItems.size() == 0) {
                        // delete the image
                        handlePictureFromCamera.handleDeleteImage(false, uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this);
                        // delete the google calendar's event
                        Helper.deleteGoogleCalendarEvent(
                            service,
                            localDB,
                            googleCalendarEvent,
                            weekPlanningCalendarId,
                            idGoogleCalendarEvent,
                            dateSelected,
                            getString(R.string.constant_breakfast)
                        );
                    } else {
                        // insert or update a google calendar's event
                        insertUpdateMealGoogleCalendar();
                    }
                    // set the result of the activityForResult and terminate the activity
                    finishActivity();
                } else {
                    Log.e("ERROR END_TIME IS NEAR TO THE END OR FINISHED =======> ", timeEvent.getTimeEnd());
                    helper.displayWithDialog(
                        AddBreakfast.this,
                        getString(R.string.error_time_event_end_near_title),
                        getString(R.string.error_time_event_end_near_message)
                    );
                }
            } else {
                // warn the user than the startTime must be less than endTime
                helper.displayWithDialog(
                    AddBreakfast.this,
                    getString(R.string.error_timestart_less_than_timeend_title),
                    getString(R.string.error_timestart_less_than_timeend_message)
                );
            }
        } else if(listFoodItems.size() > 0) {
            // if there are meal's items and the startTime or the endTime is not set,
            // display an error message
            helper.displayWithDialog(
                AddBreakfast.this,
                getString(R.string.error_time_not_selected_title),
                getString(R.string.error_time_not_selected_message)
            );
        } else {
            // if there are no meal's items and the time is not set,
            // than terminate the activity
            finishActivity();
        }
    }

    // terminate the activity
    private void finishActivity() {
        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }

    @Override
    public void onBackPressed() {
        // if there were changes, than warn the user about this with a dialog
        Helper.handleBackButtonAddMeal(
            AddBreakfast.this,
            AddBreakfast.this,
            listFoodItemsToDelete.size(),
            listFoodItemsNew.size(),
            timeEvent
        );
    }
}
