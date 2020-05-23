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

import java.time.LocalDate;
import java.util.ArrayList;

public class AddBreakfast extends AppCompatActivity implements View.OnClickListener {

    // class' variables
    private ArrayList<Food> listFoodItems;
    private ArrayAdapter<Food> adapter;
    private ListView listView;

    // UI elements
    private EditText editTextFood;
    private Button addFoodItemButton;
    private LocalDate dateSelected;

    // Helpers & Others
    private Helper helper = new Helper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_breakfast);

        editTextFood = findViewById(R.id.breakfast_insert_food);
        addFoodItemButton = findViewById(R.id.add_item_breakfast);

        addFoodItemButton.setOnClickListener(this);

        Intent mainActivityIntent = getIntent();
        int day = mainActivityIntent.getIntExtra("day", 1);
        int month = mainActivityIntent.getIntExtra("month", 1);
        int year = mainActivityIntent.getIntExtra("year", 2020);

        dateSelected = LocalDate.of(year, month, day);

        listView = findViewById(R.id.breakfast_list_food_items);

        listFoodItems = new ArrayList<Food>();
        adapter = new ArrayAdapter<Food>(this, android.R.layout.simple_list_item_1, listFoodItems);
        listView.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.add_item_breakfast:
                onAddItem();
        }
    }

    public void onAddItem() {
        String nameFoodItem = editTextFood.getText().toString();
        String dateToConsume = helper.convertDateToString(this.dateSelected);
        Food food = new Food(nameFoodItem, dateToConsume, "breakfast");

        listFoodItems.add(0, food);
        adapter.notifyDataSetChanged();

        //long idItem = DBAdapter.getInstance(AddBreakfast.this).insert(food);
        //food.setId(idItem);

        editTextFood.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(
            AddBreakfast.this.INPUT_METHOD_SERVICE
        );

        imm.hideSoftInputFromWindow(editTextFood.getApplicationWindowToken(), 0);
        editTextFood.clearFocus();
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
