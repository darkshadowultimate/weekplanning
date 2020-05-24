package it.unindubria.pdm.weekplanning;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;

public class AddBreakfast extends AppCompatActivity implements View.OnClickListener {

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference();
    private String uid;

    // class' variables
    private ArrayList<Food> listFoodItems;
    private ArrayAdapter<Food> adapter;
    private ListView listView;
    private boolean firstTimeLoadDataFromDB = true;

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private Button saveButton;
    private String dateSelected;

    // Helpers & Others
    private Helper helper = new Helper();

    @Override
    public void onStart() {
        super.onStart();

        /*
            FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

            if(mUser == null) {
                startActivity(helper.changeActivity(this, LogIn.class));
            } else {
                uid = mUser.getUid();
            }
        */
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_breakfast);

        editTextFood = findViewById(R.id.breakfast_insert_food);
        addFoodItemButton = findViewById(R.id.add_item_breakfast);
        saveButton = findViewById(R.id.breakfast_save_button);

        addFoodItemButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        Intent mainActivityIntent = getIntent();
        int day = mainActivityIntent.getIntExtra("day", 1);
        int month = mainActivityIntent.getIntExtra("month", 1);
        int year = mainActivityIntent.getIntExtra("year", 2020);


        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser == null) {
            startActivity(helper.changeActivity(this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
            mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        }

        dateSelected = helper.convertDateToString(LocalDate.of(year, month, day));

        listView = findViewById(R.id.breakfast_list_food_items);

        setUpRealtimeDBListener();

        listFoodItems = new ArrayList<Food>();
        adapter = new ArrayAdapter<Food>(this, android.R.layout.simple_list_item_1, listFoodItems);
        listView.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.add_item_breakfast:
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
            case R.id.breakfast_save_button:
                saveInfoToDB();
                break;
        }
    }

    public void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();
        Food food = new Food(nameFoodItem, dateSelected, "breakfast");

        listFoodItems.add(0, food);
        adapter.notifyDataSetChanged();

        //long idItem = DBAdapter.getInstance(AddBreakfast.this).insert(food);
        //food.setId(idItem);

        // The user could add more items at the time.
        // Clearing the focus and hiding the keyboard would be bad UX.
        editTextFood.setText("");
    }

    private void saveInfoToDB() {
        if(listFoodItems.size() > 0) {
            DatabaseReference dbRef = mDatabaseRef
                    .child(uid)
                    .child(dateSelected)
                    .child("breakfast")
                    .push();

            dbRef.setValue(listFoodItems.get(0));

            //dbRef.setValue(foodItem);

            //listFoodItems.get(0)
        }

        finishAcitivyAndGoBack();

        //for(Food foodItem: listFoodItems) {}
    }

    private void setUpRealtimeDBListener() {
        mDatabaseRef
            .child(uid)
            .child(dateSelected)
            .child("breakfast")
            .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                if(firstTimeLoadDataFromDB && dataSnapshot.getChildren() != null) {
                    for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        listFoodItems.add(0, snapshot.getValue(Food.class));
                    }

                    adapter.notifyDataSetChanged();
                    firstTimeLoadDataFromDB = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });
    }

    private void finishAcitivyAndGoBack() {
        startActivity(helper.changeActivity(AddBreakfast.this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        finishAcitivyAndGoBack();
    }
}
