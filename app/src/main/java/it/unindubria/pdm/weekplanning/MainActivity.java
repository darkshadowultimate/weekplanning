package it.unindubria.pdm.weekplanning;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;

import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // class' variables
    private int selectedDay;
    private int selectedMonth;
    private int selectedYear;

    // UI elements
    private CalendarView calendar;
    private Button buttonAddBreakfast;
    private Button buttonAddLunch;
    private Button buttonAddDinner;

    // Helpers & Others
    private Helper helper = new Helper();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(helper.changeActivity(this, LogIn.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setDefaultValueDateSelected();

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);

        calendar = (CalendarView) findViewById(R.id.calendar);
        buttonAddBreakfast = findViewById(R.id.mainactivity_add_breakfast_button);
        buttonAddLunch = findViewById(R.id.mainactivity_add_lunch_button);
        buttonAddDinner = findViewById(R.id.mainactivity_add_dinner_button);

        buttonAddBreakfast.setOnClickListener(this);
        buttonAddLunch.setOnClickListener(this);
        buttonAddDinner.setOnClickListener(this);
        setListernerCalendarView();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.mainactivity_add_breakfast_button:
                Intent breakfastIntent = helper.changeActivity(MainActivity.this, AddBreakfast.class);
                breakfastIntent.putExtra("day", selectedDay);
                breakfastIntent.putExtra("month", selectedMonth);
                breakfastIntent.putExtra("year", selectedYear);

                startActivity(breakfastIntent);
                break;
            case R.id.mainactivity_add_lunch_button:
            case R.id.mainactivity_add_dinner_button:
                startActivity(helper.changeActivity(MainActivity.this, AddBreakfast.class));
                break;
        }
    }

    private void setDefaultValueDateSelected() {
        LocalDateTime now = LocalDateTime.now();

        selectedYear = now.getYear();
        selectedMonth = now.getMonthValue();
        selectedDay = now.getDayOfMonth();
    }

    private void setListernerCalendarView() {
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                selectedDay = dayOfMonth;
                selectedMonth = month + 1;
                selectedYear = year;
                // retrieve data from database
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

    private void logout() {
        helper.displayWithToast(this, R.string.logout);
        FirebaseAuth.getInstance().signOut();
        startActivity(helper.changeActivity(this, LogIn.class));
    }
}
