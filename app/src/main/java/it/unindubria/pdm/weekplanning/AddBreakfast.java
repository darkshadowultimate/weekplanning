package it.unindubria.pdm.weekplanning;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
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
    private DBAdapter localDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_breakfast);

        // setting up Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser == null) {
            startActivity(helper.changeActivity(AddBreakfast.this, LogIn.class));
        } else {
            uid = mFirebaseUser.getUid();
            mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        }

        // connect elements to UI
        editTextFood = findViewById(R.id.breakfast_insert_food);
        addFoodItemButton = findViewById(R.id.add_item_breakfast);
        saveButton = findViewById(R.id.breakfast_finish_button);
        listView = findViewById(R.id.breakfast_list_food_items);

        // setting listeners
        addFoodItemButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        handleRemoveListViewItem();

        // getting data from intent
        Intent mainActivityIntent = getIntent();
        // saving the date choosen by the user already formatted
        dateSelected = mainActivityIntent.getStringExtra("dateString");

        // open connection to local SQLite database
        localDB = DBAdapter.getInstance(AddBreakfast.this);
        localDB.open();

        // setting up listview and adapter
        listFoodItems = new ArrayList<Food>();
        adapter = new ArrayAdapter<Food>(AddBreakfast.this, android.R.layout.simple_list_item_1, listFoodItems);
        listView.setAdapter(adapter);

        // getting data from local DB SQLite
        synchronizeListFoodItemsWithLocalDB();
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
            case R.id.breakfast_finish_button:
                finishActivityAndGoBack();
        }
    }

    public void addFoodItem() {
        String nameFoodItem = editTextFood.getText().toString();
        Food food = new Food(nameFoodItem, dateSelected, "breakfast", uid);

        listFoodItems.add(0, food);
        adapter.notifyDataSetChanged();

        saveInfoToDB(food);

        // The user could add more items at the time.
        // Clearing the focus and hiding the keyboard would be bad UX.
        editTextFood.setText("");
    }

    private void saveInfoToDB(Food item) {
        if(listFoodItems.size() > 0) {
            // update local SQLite database
            long idItem = localDB.insert(item);

            item.setId(idItem);

            // update realtime database
            DatabaseReference dbRef = mDatabaseRef.child(uid).child(dateSelected).push();
            dbRef.setValue(item);
        }
    }

    private void synchronizeListFoodItemsWithLocalDB() {
        ArrayList<Food> loadedFoodItems = localDB
            .getAllFoodItemsSection(uid, dateSelected, "breakfast");

        for(Food item: loadedFoodItems) {
            listFoodItems.add(0, item);
        }

        adapter.notifyDataSetChanged();
    }

    private void handleRemoveListViewItem() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog
                    .Builder(AddBreakfast.this)
                    .setTitle("Remove element")
                    .setMessage("Do you really wanna remove this element?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            long idItem = listFoodItems.get(position).getId();

                            localDB.removeFoodItem(idItem);
                            listFoodItems.remove(position);

                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }

    private void finishActivityAndGoBack() {
        localDB.close();
        startActivity(helper.changeActivity(AddBreakfast.this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        finishActivityAndGoBack();
    }
}
