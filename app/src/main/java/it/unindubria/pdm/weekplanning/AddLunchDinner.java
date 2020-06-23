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
import android.widget.Spinner;
import android.widget.TimePicker;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddLunchDinner extends AppCompatActivity implements View.OnClickListener {
    // CONSTANTS
    private static String[] SUBCATEGORIES_VOICES_DB;
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
    private TimeEvent timeEvent;

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

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private Button timePickerStartButton;
    private Button timePickerEndButton;
    private Button takePicture;
    private Button deleteMealButton;
    private Button saveButton;
    private ImageView previewImage;
    private Spinner dropdown_subcategories;


    // Helpers & Others
    private Helper helper = new Helper();
    private HandlePictureFromCamera handlePictureFromCamera = new HandlePictureFromCamera();
    private DBAdapter localDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lunch_dinner);

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);

        // setting up Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser == null) {
            startActivity(helper.changeActivity(AddLunchDinner.this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
        }

        SUBCATEGORIES_VOICES_DB = Helper.getSUBCATEGORIES_VOICES_DB(AddLunchDinner.this);

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
        deleteMealButton = findViewById(R.id.delete_button);
        saveButton = findViewById(R.id.finish_button);
        previewImage = findViewById(R.id.preview_image_meal);
        listViewLunchDinner = findViewById(R.id.list_food_items_meal);
        dropdown_subcategories = findViewById(R.id.dropdown_subcategory);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        timePickerStartButton.setOnClickListener(this);
        timePickerEndButton.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        deleteMealButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        previewImage.setOnClickListener(this);
        handleRemoveListViewItem();

        // setting variables' values
        prioritySubCategories = new HashMap<String, Integer>();
        prioritySubCategories.put(getString(R.string.constant_before), new Integer(0));
        prioritySubCategories.put(getString(R.string.constant_first), new Integer(1));
        prioritySubCategories.put(getString(R.string.constant_second), new Integer(2));
        prioritySubCategories.put(getString(R.string.constant_after), new Integer(3));

        // getting data from intent
        Intent mainActivityIntent = getIntent();
        // saving the date choosen by the user already formatted
        dateSelected = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_dateString));
        lunchOrDinner = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_lunchOrDinner));
        weekPlanningCalendarId = mainActivityIntent.getStringExtra(getString(R.string.constant_intent_calendarId));

        toolbar.setTitle(dateSelected + " / " + getString(getResources().getIdentifier(
            "section_meal_" + lunchOrDinner,
            "string",
            getPackageName()
        )));

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

            timeEvent = new TimeEvent(
                googleCalendarEvent.getTimeStart(),
                googleCalendarEvent.getTimeEnd()
            );

            idGoogleCalendarEvent = googleCalendarEvent.getId();

            timePickerStartButton.setText(timeEvent.getTimeStart());
            timePickerEndButton.setText(timeEvent.getTimeEnd());
        } else {
            timeEvent = new TimeEvent();
            Log.e("CHECK CLASS", "googleCalendarEvent IS ABSOLUTELY NULL");
        }

        handlePictureFromCamera.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);

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
        if(GoogleAPIHelper.isDeviceOnline(AddLunchDinner.this)) {
            switch(view.getId()) {
                case R.id.add_item_meal:
                    // there can be max 20 items
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
                    handlePictureFromCamera.handleDeleteImage(true, uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this);
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
                        AddLunchDinner.this,
                        AddLunchDinner.this,
                        localDB,
                        service,
                        previewImage,
                        lunchOrDinner,
                        dateSelected,
                        uid,
                        weekPlanningCalendarId,
                        idGoogleCalendarEvent,
                        listFoodItemsLunchDinner.size(),
                        listFoodItemsNew.size(),
                        listFoodItemsToDelete.size()
                    );
                    break;
                case R.id.finish_button:
                    finishActivityAndGoBack();
                    break;
            }
        }
    }

    // get the text value of the subcategory selected from the dropdown at the top
    private String getSubcategoryStringValueForDB(String valueSelected) {
        for(int i = 0; i < SUBCATEGORIES_VOICES_DB.length; i++) {
            if(dropdown_subcategories.getItemAtPosition(i).toString().equals(valueSelected)) {
                return SUBCATEGORIES_VOICES_DB[i];
            }
        }
        return null;
    }

    // set the time by the TimePicker
    private void setTimeSelected(final boolean isStartTime) {
        helper.setTimeWithTimePicker(
            AddLunchDinner.this,
            timeEvent,
            timePickerStartButton,
            timePickerEndButton,
            isStartTime
        );
    }

    private void takePictureFromCamera() {
        Intent cameraIntent = handlePictureFromCamera.takePicture(uid, dateSelected, lunchOrDinner, AddLunchDinner.this, AddLunchDinner.this);

        if(cameraIntent != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
            // open camera to take picture
            startActivityForResult(cameraIntent, helper.getTakePictureCodeStartActivity(AddLunchDinner.this));
        }
    }

    // add new item to the listFoodItems ArrayList
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

    // get the correct position where to insert the item passed as argument in the listFoodItemsLunchDinner
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

    // get the translation for the subcategory passed as parameter (this will be the summary of the Google Calendar event)
    private String getTranslationSubcategory(String keyword) {
        int idString = getResources().getIdentifier(keyword, "string", getPackageName());
        return getString(idString);
    }

    // load data from database and update the listFoodItems ArrayList
    private void synchronizeListFoodItemsWithLocalDB() {
        ArrayList<Food> loadedFoodItems = localDB
                .getAllFoodItemsSection(uid, dateSelected, lunchOrDinner);

        sortListFoodItems(loadedFoodItems);
    }

    // sort the list food items based on their subcategory (starter, first course, second course, end of meal)
    private void sortListFoodItems(ArrayList<Food> listToOrder) {
        for (String valueCategory : SUBCATEGORIES_VOICES_DB) {
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

    // handle the creation or the update of a new event in the user's Google Calendar
    private void insertUpdateMealGoogleCalendar() {
        final String allFoodItemsStringCalendarEvent = Helper.getStringListLunchDinnerItemsForDB(
                SUBCATEGORIES_VOICES_DB, listFoodItemsLunchDinner, lunchOrDinner, AddLunchDinner.this
        );

        if(timeEvent.isTimeEventDefined() && listFoodItemsLunchDinner.size() > 0) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        updateLocalDB();
                        if(googleCalendarEvent != null) {
                            if(
                                listFoodItemsNew.size() > 0 ||
                                listFoodItemsToDelete.size() > 0 ||
                                !googleCalendarEvent.getTimeStart().equals(timeEvent.getTimeStart()) ||
                                !googleCalendarEvent.getTimeEnd().equals(timeEvent.getTimeEnd())
                            ) {
                                Log.e("CHECK FOR UPDATE EVENT", "UPDATE EVENT");

                                GoogleCalendarHelper.updateCalendarEvent(
                                        service,
                                        weekPlanningCalendarId,
                                        idGoogleCalendarEvent,
                                        dateSelected,
                                        timeEvent.getTimeStart(),
                                        timeEvent.getTimeEnd(),
                                        summaryGoogleCalendarEvent,
                                        allFoodItemsStringCalendarEvent
                                );

                                localDB.updateGoogleCalendarEvent(new GoogleCalendarEvent(
                                        idGoogleCalendarEvent,
                                        timeEvent.getTimeStart(),
                                        timeEvent.getTimeEnd(),
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
                                    timeEvent.getTimeStart(),
                                    timeEvent.getTimeEnd()
                            );

                            localDB.insertGoogleCalendarEvent(new GoogleCalendarEvent(
                                    idGoogleCalendarEvent,
                                    timeEvent.getTimeStart(),
                                    timeEvent.getTimeEnd(),
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
        // request permission for camera
        if (requestCode == helper.getCameraPermissionCode(AddLunchDinner.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                helper.displayWithToast(AddLunchDinner.this, getString(R.string.success_can_take_picture));
            } else {
                helper.displayWithToast(AddLunchDinner.this, getString(R.string.error_cannot_take_picture));
            }
        // request permission for reading/writing on storage
        } else if (requestCode == helper.getStoragePermissionCode(AddLunchDinner.this)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // load image's preview (if exists) from internal storage
                handlePictureFromCamera.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);
            } else {
                helper.displayWithToast(AddLunchDinner.this, getString(R.string.error_cannot_save_picture));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // finished activity for taking picture by the camera
        if (requestCode == helper.getTakePictureCodeStartActivity(AddLunchDinner.this) && resultCode == RESULT_OK) {
            handlePictureFromCamera.setPreviewImage(uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this, AddLunchDinner.this);
        }
    }

    // handle mechanism to remove an element selected from the listView
    private void handleRemoveListViewItem() {
        listViewLunchDinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Helper.handleRemoveListViewItem(
                    AddLunchDinner.this,
                    listFoodItemsLunchDinner,
                    listFoodItemsNew,
                    listFoodItemsToDelete,
                    adapterLunchDinner,
                    position
                );
            }
        });
    }

    private void updateLocalDB() {
        Helper.addAllFoodItemsToDBWhichWereAdded(localDB, listFoodItemsNew);
        Helper.deleteAllFoodItemsFromDBWhichWereRemoved(localDB, listFoodItemsToDelete);
    }

    // check all cases in order to not create inconsistency and update SQLite local db and Google Calendar
    // (than terminate the activity)
    private void finishActivityAndGoBack() {
        Log.e("CONDITION START_TIME & END_TIME =======> ", String.valueOf(timeEvent.isTimeEventDefined()));
        // startTime and endTime must have a value
        if(timeEvent.isTimeEventDefined()) {
            Log.e("START_TIME AND END_TIME HAVE VALUES", timeEvent.getTimeStart() + " ---- " + timeEvent.getTimeEnd());
            // startTime must be less than endTime
            if(timeEvent.isTimeStartLessThanTimeEnd()) {
                // if there are no meal's items,
                // than delete the google calendar's event and the meal's picture (if they exist)
                // otherwise update the SQLite DB and the google calendar's event
                if(listFoodItemsLunchDinner.size() == 0 || timeEvent.isTimeEventEndInTheFuture(dateSelected)) {
                    if(listFoodItemsLunchDinner.size() == 0) {
                        Log.e("deleteGoogleCalendarEvent", "CALL FUNCTION TO DELETE");
                        updateLocalDB();
                        // delete the image
                        handlePictureFromCamera.handleDeleteImage(false, uid, dateSelected, lunchOrDinner, previewImage, AddLunchDinner.this);
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
                    finishActivity();
                } else {
                    Log.e("ERROR END_TIME IS NEAR TO THE END OR FINISHED =======> ", timeEvent.getTimeEnd());
                    helper.displayWithDialog(
                            AddLunchDinner.this,
                            getString(R.string.error_time_event_end_near_title),
                            getString(R.string.error_time_event_end_near_message)
                    );
                }
            } else {
                Log.e("ERROR START_TIME > END_TIME =======> ", timeEvent.getTimeStart() + " ---- " + timeEvent.getTimeEnd());
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
                (timeEvent.getTimeStart() == null ? "null" : timeEvent.getTimeStart()) +
                " ---- " +
                (timeEvent.getTimeEnd() == null ? "null" : timeEvent.getTimeEnd())
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
            AddLunchDinner.this,
            AddLunchDinner.this,
            listFoodItemsToDelete.size(),
            listFoodItemsNew.size(),
            timeEvent
        );
    }
}
