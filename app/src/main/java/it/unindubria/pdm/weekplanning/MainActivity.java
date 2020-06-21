package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.calendar.Calendar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // CONSTANTS
    private final String[] SUBCATEGORIES_VOICES_DB = { "before", "first", "second", "after" };

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;
    // Google Calendar API
    private Calendar service;
    private String weekPlanningCalendarId = "";

    // class' variables
    private String selectedDateString;

    // UI elements
    private CalendarView calendar;
    private Button buttonAddBreakfast;
    private Button buttonAddLunch;
    private Button buttonAddDinner;
    // -- Breakfast --
    private LinearLayout breakfastCard;
    private TextView titleBreakfastCard;
    private TextView foodItemBreakfast;
    // -- Lunch --
    private TextView partOfMealSection;
    private LinearLayout lunchCard;
    // -- Dinner --
    private LinearLayout dinnerCard;

    // Helpers & Others
    private Helper helper = new Helper();
    private DBAdapter localDB;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setDefaultValueDateSelected();

        // setting up Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser == null) {
            startActivity(helper.changeActivity(MainActivity.this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
            helper.createNewDirectory("/WeekPlanning/" + uid);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);

        service = GoogleCalendarHelper.getCalendarBuilderInstance(
                MainActivity.this,
                mFirebaseUser.getEmail()
        );

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(MainActivity.this);
        localDB.openRead();

        calendar = (CalendarView) findViewById(R.id.calendar);
        buttonAddBreakfast = findViewById(R.id.mainactivity_add_breakfast_button);
        buttonAddLunch = findViewById(R.id.mainactivity_add_lunch_button);
        buttonAddDinner = findViewById(R.id.mainactivity_add_dinner_button);

        breakfastCard = findViewById(R.id.single_card_breakfast);
        titleBreakfastCard = findViewById(R.id.title_breakfast_card);
        foodItemBreakfast = findViewById(R.id.food_items_part_breakfast);

        lunchCard = findViewById(R.id.single_card_lunch);
        dinnerCard = findViewById(R.id.single_card_dinner);

        buttonAddBreakfast.setOnClickListener(this);
        buttonAddLunch.setOnClickListener(this);
        buttonAddDinner.setOnClickListener(this);
        breakfastCard.setOnClickListener(this);
        lunchCard.setOnClickListener(this);
        dinnerCard.setOnClickListener(this);

        setListernerCalendarView();

        updateUI();
    }

    private void createNewGoogleCalendar() {
        try {
            String calendarId = GoogleCalendarHelper.createNewCalendar(
                    service,
                    uid,
                    getString(R.string.google_calendar_description)
            );
            if (calendarId != null) {
                localDB.insertNewUserCalendar(uid, calendarId);
                weekPlanningCalendarId = calendarId;
            }
            Log.d("CREATE_NEW_CALENDAR", String.valueOf(calendarId));
        } catch(UserRecoverableAuthIOException exc) {
            startActivityForResult(
                exc.getIntent(),
                helper.getGoogleCalendarCodeStartActivity(MainActivity.this)
            );
        } catch(IOException exc) {
            Log.e("CREATE_NEW_CALENDAR", "CANNOT CREATE CALENDAR", exc);
        }
    }

    private void handleCreateNewGoogleCalendar() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String calendarId = localDB.getCalendarId(uid);
                    Log.e("CHECK IF CALENDAR ID EXISTS FROM LOCAL DB =======> ", calendarId == null ? "NULL" : calendarId);
                    // check if the calendar exists
                    if(calendarId == null) {
                        createNewGoogleCalendar();
                    } else {
                        com.google.api.services.calendar.model.Calendar cal = service.calendars().get(calendarId).execute();
                        weekPlanningCalendarId = calendarId;
                        Log.e("GET EXISTING CALENDAR =======> ", weekPlanningCalendarId);
                    }
                } catch(IOException exc) {
                    Log.e("EXCEPTION RETRIEVE CALENDAR", "CALENDAR NOT FOUND");
                    createNewGoogleCalendar();
                }
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        handleCreateNewGoogleCalendar();
    }

    private void updateUI() {
        ArrayList<Food> foodDateList = localDB.getAllFoodItemsDate(uid, selectedDateString);

        updateBreakfast(foodDateList);
        updateLunch(foodDateList, getString(R.string.constant_lunch));
        updateLunch(foodDateList, getString(R.string.constant_dinner));
    }

    private void updateBreakfast(ArrayList<Food> foodItems) {
        String allItemsString = helper.getStringListBreakfastItem(foodItems);

        foodItemBreakfast.setText(allItemsString);

        if(!allItemsString.isEmpty()) {
            breakfastCard.setVisibility(View.VISIBLE);
            buttonAddBreakfast.setVisibility(View.GONE);
        } else {
            buttonAddBreakfast.setVisibility(View.VISIBLE);
            breakfastCard.setVisibility(View.GONE);
        }
    }

    private void updateLunch(ArrayList<Food> foodItems, String lunchOrDinner) {
        boolean isThereLunch = false;

        for(String subcategory: SUBCATEGORIES_VOICES_DB) {
            String allItems = "";

            for(Food item: foodItems) {
                if(item.getCategory().equals(lunchOrDinner) && item.getSubcategory().equals(subcategory)) {
                    allItems += ". " + item.getName() + "\n";
                }
            }

            String idView = lunchOrDinner + "_part_meal_" + subcategory;
            partOfMealSection = findViewById(
                getResources()
                .getIdentifier(idView, "id", MainActivity.this.getPackageName())
            );
            // clear the text in the card
            partOfMealSection.setText("");

            if(!allItems.isEmpty()) {
                isThereLunch = true;
                partOfMealSection.setText(allItems);
                partOfMealSection.setVisibility(View.VISIBLE);
            }
        }

        if(isThereLunch) {
            if(lunchOrDinner.equals(getString(R.string.constant_lunch))) {
                lunchCard.setVisibility(View.VISIBLE);
                buttonAddLunch.setVisibility(View.GONE);
            } else {
                dinnerCard.setVisibility(View.VISIBLE);
                buttonAddDinner.setVisibility(View.GONE);
            }
        } else {
            if(lunchOrDinner.equals(getString(R.string.constant_lunch))) {
                lunchCard.setVisibility(View.GONE);
                buttonAddLunch.setVisibility(View.VISIBLE);
            } else {
                dinnerCard.setVisibility(View.GONE);
                buttonAddDinner.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startActivityForResultMeal(String typeOfMeal) {
        if(typeOfMeal.equals(getString(R.string.constant_breakfast))) {
            Log.e("READY TO START BREAKFAST ACTIVITY ======> ", typeOfMeal);
            Intent breakfastIntent = new Intent(MainActivity.this, AddBreakfast.class);
            breakfastIntent.putExtra(
                getString(R.string.constant_intent_dateString),
                selectedDateString
            );
            breakfastIntent.putExtra(
                getString(R.string.constant_intent_calendarId),
                weekPlanningCalendarId);
            startActivityForResult(breakfastIntent, helper.getBreakfastCodeStartActivity(MainActivity.this));
        } else {
            Log.e("READY TO START LUNCH/DINNER ACTIVITY ======> ", typeOfMeal);
            Intent lunchIntent = new Intent(MainActivity.this, AddLunchDinner.class);
            lunchIntent.putExtra(
                    getString(R.string.constant_intent_dateString),
                    selectedDateString
            );
            lunchIntent.putExtra(
                getString(R.string.constant_intent_lunchOrDinner),
                typeOfMeal
            );
            lunchIntent.putExtra(
                getString(R.string.constant_intent_calendarId),
                weekPlanningCalendarId
            );
            startActivityForResult(lunchIntent, helper.getLunchDinnerCodeStartActivity(MainActivity.this));
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.single_card_breakfast:
            case R.id.mainactivity_add_breakfast_button:
                startActivityForResultMeal(getString(R.string.constant_breakfast));
                break;
            case R.id.single_card_lunch:
            case R.id.mainactivity_add_lunch_button:
                startActivityForResultMeal(getString(R.string.constant_lunch));
                break;
            case R.id.single_card_dinner:
            case R.id.mainactivity_add_dinner_button:
                startActivityForResultMeal(getString(R.string.constant_dinner));
                break;
        }
    }

    private void setDefaultValueDateSelected() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        int dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        int month = calendar.get(java.util.Calendar.MONTH);
        int year = calendar.get(java.util.Calendar.YEAR);

        selectedDateString = helper.getStringDate(dayOfMonth, month + 1, year);
    }

    private void setListernerCalendarView() {
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                selectedDateString = helper.getStringDate(dayOfMonth, month + 1, year);
                updateUI();
            }
        });
    }

    private void requireGoogleCalendarPermissions() {
        new AlertDialog
            .Builder(MainActivity.this)
            .setTitle(getString(R.string.warning_permission_calendar_title))
            .setMessage(getString(R.string.warning_permission_calendar_message))
            .setPositiveButton(getString(R.string.button_allow), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handleCreateNewGoogleCalendar();
                }
            })
            .setNegativeButton(getString(R.string.button_negate), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    helper.displayWithToast(
                    MainActivity.this,
                        getString(R.string.error_unable_retrieve_permissions)
                    );
                    finish();
                }
            })
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.logout:
                new AlertDialog
                    .Builder(MainActivity.this)
                    .setTitle(getString(R.string.warning_logout_title))
                    .setMessage(getString(R.string.warning_logout_message))
                    .setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            logout();
                        }
                    })
                    .setNegativeButton(getString(R.string.button_no), null)
                    .show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (
            requestCode == helper.getBreakfastCodeStartActivity(MainActivity.this) ||
            requestCode == helper.getLunchDinnerCodeStartActivity(MainActivity.this)
        ) {
            if(resultCode == Activity.RESULT_OK) {
                updateUI();
            }
            if (resultCode == Activity.RESULT_CANCELED) {}
        } else if(requestCode == helper.getGoogleCalendarCodeStartActivity(MainActivity.this)) {
            if(resultCode == RESULT_OK) {
                handleCreateNewGoogleCalendar();
            } else {
                requireGoogleCalendarPermissions();
            }
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInClient googleClient = GoogleAPIHelper.getGoogleSignInClient(getString(R.string.default_web_client_id), MainActivity.this);
        // Google sign out
        googleClient
            .signOut()
            .addOnCompleteListener(
                this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        startActivity(helper.changeActivity(MainActivity.this, LogIn.class));
                    }
                }
            );
    }
}
