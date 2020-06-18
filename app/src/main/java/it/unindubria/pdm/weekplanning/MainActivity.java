package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
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
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // CONSTANTS
    private static final int ADD_ITEMS_BREAKFAST = 1;
    private static final int ADD_ITEMS_LUNCH_DINNER = 2;
    private static final int REQUEST_CODE_NEED_PERMISSION = 222;
    private final String[] SUBCATEGORIES_VOICES_DB = { "before", "first", "second", "after" };

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;
    // Google Calendar API
    private Calendar service;

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

        /*
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean result = GoogleCalendarHelper.createNewCalendar(service);
                    Log.d("CREATE_NEW_CALENDAR", String.valueOf(result));
                }
            });
        */

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

    private void insertMealGoogleCalendar(
            final String summary,
            final String description,
            final String date,
            final String timeStart,
            final String timeEnd
    ) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    GoogleCalendarHelper.createNewEvent(service, summary, description, date, timeStart, timeEnd);
                } catch(UserRecoverableAuthIOException exc) {
                    MainActivity.this.startActivityForResult(
                        exc.getIntent(),
                        MainActivity.REQUEST_CODE_NEED_PERMISSION
                    );
                } catch (IOException exc) {
                    Log.e("CALENDAR INFO", "ERROR CALENDAR READING ID", exc);
                }
            }
        }).start();
    }

    private void updateUI() {
        ArrayList<Food> foodDateList = localDB.getAllFoodItemsDate(uid, selectedDateString);

        updateBreakfast(foodDateList);
        updateLunch(foodDateList, "lunch");
        updateLunch(foodDateList, "dinner");
    }

    private void updateBreakfast(ArrayList<Food> foodItems) {
        String allItems = "";

        for(Food item: foodItems) {
            if(item.getCategory().equals("breakfast")) {
                allItems += "- " + item.getName() + "\n";
            }
        }
        foodItemBreakfast.setText(allItems);

        if(!allItems.isEmpty()) {
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

            if(allItems.isEmpty()) {
                //partOfMealSection.setVisibility(View.GONE);
            } else {
                isThereLunch = true;
                partOfMealSection.setText(allItems);
                partOfMealSection.setVisibility(View.VISIBLE);
            }
        }

        if(isThereLunch) {
            if(lunchOrDinner.equals("lunch")) {
                lunchCard.setVisibility(View.VISIBLE);
                buttonAddLunch.setVisibility(View.GONE);
            } else {
                dinnerCard.setVisibility(View.VISIBLE);
                buttonAddDinner.setVisibility(View.GONE);
            }
        } else {
            if(lunchOrDinner.equals("lunch")) {
                lunchCard.setVisibility(View.GONE);
                buttonAddLunch.setVisibility(View.VISIBLE);
            } else {
                dinnerCard.setVisibility(View.GONE);
                buttonAddDinner.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.single_card_breakfast:
            case R.id.mainactivity_add_breakfast_button:
                Intent breakfastIntent = new Intent(MainActivity.this, AddBreakfast.class);
                breakfastIntent.putExtra("dateString", selectedDateString);
                startActivityForResult(breakfastIntent, ADD_ITEMS_BREAKFAST);
                break;
            case R.id.single_card_lunch:
            case R.id.mainactivity_add_lunch_button:
                Intent lunchIntent = new Intent(MainActivity.this, AddLunchDinner.class);
                lunchIntent.putExtra("dateString", selectedDateString);
                lunchIntent.putExtra("lunchOrLunch", "lunch");
                startActivityForResult(lunchIntent, ADD_ITEMS_LUNCH_DINNER);
                break;
            case R.id.single_card_dinner:
            case R.id.mainactivity_add_dinner_button:
                Intent dinnerIntent = new Intent(MainActivity.this, AddLunchDinner.class);
                dinnerIntent.putExtra("dateString", selectedDateString);
                dinnerIntent.putExtra("lunchOrLunch", "dinner");
                startActivityForResult(dinnerIntent, ADD_ITEMS_LUNCH_DINNER);
                break;
        }
    }

    private void setDefaultValueDateSelected() {
        LocalDateTime now = LocalDateTime.now();
        selectedDateString = helper.getStringDate(now.getDayOfMonth(), now.getMonthValue(), now.getYear());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.logout:
                logout();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_ITEMS_BREAKFAST || requestCode == ADD_ITEMS_LUNCH_DINNER) {
            if(resultCode == Activity.RESULT_OK) {
                updateUI();
            }
            if (resultCode == Activity.RESULT_CANCELED) {}
        }
    }

    private void logout() {
        helper.displayWithToast(this, R.string.logout);

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
