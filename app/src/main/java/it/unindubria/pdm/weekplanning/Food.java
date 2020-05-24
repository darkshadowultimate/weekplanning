package it.unindubria.pdm.weekplanning;

import android.content.ContentValues;

public class Food {

    private long id = 10;
    private String name;
    private String consumationDate;
    private String category;

    // in order to work with DataSnapshot (Firebase),
    // this class must have a default constructor with no arguments
    public Food() {}

    public Food(String name, String consumationDate, String category) {
        this.name = name;
        this.consumationDate = consumationDate;
        this.category = category;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConsumationDate() {
        return consumationDate;
    }

    public String getCategory() {
        return category;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConsumationDate(String consumationDate) {
        this.consumationDate = consumationDate;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ContentValues obtainAsContentValue() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DBContract.FoodItems.FOODS_NAME, this.name);
        contentValues.put(DBContract.FoodItems.FOODS_CONSUMATIONDATE, this.consumationDate);
        contentValues.put(DBContract.FoodItems.FOODS_CATEGORY, this.category);

        return contentValues;
    }

    @Override
    public String toString() {
        return name + " - " + consumationDate;
    }
}
