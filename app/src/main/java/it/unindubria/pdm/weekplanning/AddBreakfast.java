package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
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
    private Button saveButton;
    private ImageView previewImage;
    private String dateSelected;

    // Helpers & Others
    private Helper helper = new Helper();
    private HandlePictureFromCamera handlePictureFromCamera = new HandlePictureFromCamera();
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
        dateSelected = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_dateString));
        weekPlanningCalendarId = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_calendarId));

        toolbar.setTitle(dateSelected + " / " + getString(R.string.section_meal_breakfast));

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(AddBreakfast.this);
        localDB.openWrite();

        // loading values of startTime and endTime from localDB
        googleCalendarEvent = localDB.getGoogleCalendarEvent(dateSelected, getString(R.string.constant_breakfast));

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

        handlePictureFromCamera.setPreviewImage(uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this, AddBreakfast.this);

        // setting up listview and adapter
        listFoodItems = new ArrayList<Food>();
        listFoodItemsNew = new ArrayList<Food>();
        listFoodItemsToDelete = new ArrayList<Food>();
        adapter = new ArrayAdapter<Food>(AddBreakfast.this, android.R.layout.simple_list_item_1, listFoodItems);
        listView.setAdapter(adapter);

        // getting data from local DB SQLite
        synchronizeListFoodItemsWithLocalDB();
    }

    @Override
    public void onClick(View view) {
        if(GoogleAPIHelper.isDeviceOnline(AddBreakfast.this)) {
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
                case R.id.finish_button:
                    finishActivityAndGoBack();
            }
        }
    }

    private void insertUpdateMealGoogleCalendar() {
        final String allFoodItemsStringDB = helper.getStringListBreakfastItemForDB(listFoodItems);
        final String allFoodItemsStringCalendarEvent = helper.getStringListBreakfastItem(listFoodItems);

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

    private void setTimeSelected(final boolean isStartTime) {
        helper.setTimeWithTimePicker(
                AddBreakfast.this,
                timeEvent,
                timePickerStartButton,
                timePickerEndButton,
                isStartTime
        );
    }

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
            startActivityForResult(cameraIntent, helper.getTakePictureCodeStartActivity(AddBreakfast.this));
        }
    }

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
        if (requestCode == helper.getCameraPermissionCode(AddBreakfast.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                helper.displayWithToast(AddBreakfast.this, getString(R.string.success_can_take_picture));
            } else {
                helper.displayWithToast(AddBreakfast.this, getString(R.string.error_cannot_take_picture));
            }
        } else if (requestCode == helper.getStoragePermissionCode(AddBreakfast.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePictureFromCamera.setPreviewImage(uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this, AddBreakfast.this);
            } else {
                helper.displayWithToast(AddBreakfast.this, getString(R.string.error_cannot_save_picture));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == helper.getTakePictureCodeStartActivity(AddBreakfast.this) && resultCode == RESULT_OK) {
            handlePictureFromCamera.setPreviewImage(uid, dateSelected, getString(R.string.constant_breakfast), previewImage, AddBreakfast.this, AddBreakfast.this);
        } else if(requestCode == helper.getGoogleCalendarCodeStartActivity(AddBreakfast.this) && resultCode == RESULT_OK) {
            insertUpdateMealGoogleCalendar();
        }
    }

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

    private void finishActivityAndGoBack() {
        // startTime and endTime must have a value
        if(timeEvent.isTimeEventDefined()) {
            // startTime must be less than endTime
            if(timeEvent.isTimeStartLessThanTimeEnd()) {
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
                setResult(Activity.RESULT_OK, new Intent());
                finish();
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
