package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // CONSTANTS
    private static final int ADD_ITEMS_BREAKFAST = 1;
    private static final int ADD_ITEMS_LUNCH_DINNER = 2;

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String uid;

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
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(MainActivity.this);
        localDB.openRead();

        calendar = (CalendarView) findViewById(R.id.calendar);
        buttonAddBreakfast = findViewById(R.id.mainactivity_add_breakfast_button);
        buttonAddLunch = findViewById(R.id.mainactivity_add_lunch_button);
        //buttonAddDinner = findViewById(R.id.mainactivity_add_dinner_button);

        breakfastCard = findViewById(R.id.single_card_breakfast);
        titleBreakfastCard = findViewById(R.id.title_breakfast_card);
        foodItemBreakfast = findViewById(R.id.food_items_part_breakfast);

        buttonAddBreakfast.setOnClickListener(this);
        buttonAddLunch.setOnClickListener(this);
        //buttonAddDinner.setOnClickListener(this);
        breakfastCard.setOnClickListener(this);
        setListernerCalendarView();

        updateUI();
    }

    private void updateUI() {
        ArrayList<Food> foodDateList = localDB.getAllFoodItemsDate(uid, selectedDateString);

        //helper.dispayWithLog("UPDATEUI ====> ", "UPDATEUI -----" + foodDateList.toString());

        updateBreakfast(foodDateList);
    }

    private void updateBreakfast(ArrayList<Food> foodItems) {
        String allItems = "";

        if(foodItems.size() > 0) {
            for(Food item: foodItems) {
                if(item.getCategory().equals("breakfast")) {
                    allItems += "- " + item.getName() + "\n";
                }
            }
            foodItemBreakfast.setText(allItems);
        }

        if(allItems.isEmpty()) {
            if(buttonAddBreakfast.getVisibility() == View.GONE) {
                breakfastCard.setVisibility(View.GONE);
                buttonAddBreakfast.setVisibility(View.VISIBLE);
            }
        } else {
            breakfastCard.setVisibility(View.VISIBLE);
            buttonAddBreakfast.setVisibility(View.GONE);
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
            case R.id.mainactivity_add_lunch_button:
                Intent lunchIntent = new Intent(MainActivity.this, AddLunchDinner.class);
                lunchIntent.putExtra("dateString", selectedDateString);
                lunchIntent.putExtra("lunchOrLunch", "lunch");
                startActivityForResult(lunchIntent, ADD_ITEMS_LUNCH_DINNER);
                break;
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
        startActivity(helper.changeActivity(this, LogIn.class));
    }
}
