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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference();
    private String uid;

    // class' variables
    private String selectedDateString;

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
        } else {
            mFirebaseAuth = FirebaseAuth.getInstance();
            mFirebaseUser = mFirebaseAuth.getCurrentUser();

            if(mFirebaseUser == null) {
                startActivity(helper.changeActivity(this, LogIn.class));
            } else {
                uid = mFirebaseUser.getUid();
                mDatabaseRef = FirebaseDatabase.getInstance().getReference();
            }
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
                breakfastIntent.putExtra("dateString", selectedDateString);
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
        selectedDateString = helper.getStringDate(now.getDayOfMonth(), now.getMonthValue(), now.getYear());
    }

    private void setListernerCalendarView() {
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                selectedDateString = helper.getStringDate(dayOfMonth, month + 1, year);
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
