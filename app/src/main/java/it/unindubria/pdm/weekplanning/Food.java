package it.unindubria.pdm.weekplanning;

import android.content.ContentValues;

public class Food {

    private long id;
    private String name;
    private String consumationDate;
    private String category;
    private String subcategory;
    private String userId;

    // in order to work with DataSnapshot (Firebase),
    // this class must have a default constructor with no arguments
    public Food() {}

    public Food(String name, String consumationDate, String category, String userId) {
        this.name = name;
        this.consumationDate = consumationDate;
        this.category = category;
        this.subcategory = "";
        this.userId = userId;
    }

    public Food(String name, String consumationDate, String category, String subcategory, String userId) {
        this.name = name;
        this.consumationDate = consumationDate;
        this.category = category;
        this.subcategory = subcategory;
        this.userId = userId;
    }

    public Food(long id, String name, String consumationDate, String category, String subcategory, String userId) {
        this.id = id;
        this.name = name;
        this.consumationDate = consumationDate;
        this.category = category;
        this.subcategory = subcategory;
        this.userId = userId;
    }

    public Food(long id, String name, String consumationDate, String category, String userId) {
        this.id = id;
        this.name = name;
        this.consumationDate = consumationDate;
        this.category = category;
        this.subcategory = "";
        this.userId = userId;
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

    public String getSubcategory() {
        return subcategory;
    }

    public String getUserId() {
        return userId;
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

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ContentValues obtainAsContentValue() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DBContract.FoodItems.FOODS_NAME, this.name);
        contentValues.put(DBContract.FoodItems.FOODS_CONSUMATIONDATE, this.consumationDate);
        contentValues.put(DBContract.FoodItems.FOODS_CATEGORY, this.category);

        if(!this.subcategory.isEmpty()) {
            contentValues.put(DBContract.FoodItems.FOODS_SUBCATEGORY, this.subcategory);
        }

        contentValues.put(DBContract.FoodItems.FOODS_USER, this.userId);

        return contentValues;
    }

    @Override
    public String toString() {
        return name + (!this.subcategory.isEmpty() ? " - " + this.subcategory : "");
    }
}
