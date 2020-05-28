package it.unindubria.pdm.weekplanning;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

public class DBAdapter {

    private static DBAdapter sInstance;
    private SQLiteDatabase db;
    private DBHelper dbHelper;

    public static synchronized DBAdapter getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DBAdapter(context.getApplicationContext());
        return sInstance;
    }

    private DBAdapter(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public DBAdapter openWrite() throws SQLException {
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public DBAdapter openRead() {
        db = dbHelper.getReadableDatabase();
        return this;
    }

    public void close() {
        db.close();
    }

    public long insert(Food foodItem) {
        long idFoodItem = db.insert(
                DBContract.FoodItems.FOODS_TABLE,
                null,
                foodItem.obtainAsContentValue());
        return idFoodItem;
    }

    public String debugAllRecordsFoodTable() {
        Cursor cursor = db.query(
                DBContract.FoodItems.FOODS_TABLE,
                DBContract.FoodItems.FOODS_COLUMNS,
                null, null,
                null, null, null, null
        );
        String result = "";

        if (cursor.moveToFirst()) {
            do {
                long id = Long.parseLong(cursor.getString(0));
                String name = cursor.getString(1);
                String consumationDate = cursor.getString(2);
                String categoryText = cursor.getString(3);
                String userid = cursor.getString(4);

                result += String.valueOf(id) + " -- " + name + " -- " + consumationDate + " -- " + categoryText + " -- " + userid + "  *******  ";
            } while (cursor.moveToNext());
        }

        return result;
    }

    public ArrayList<Food> getAllFoodItemsSection(String userId, String date, String category) {
        ArrayList<Food> listFoodItemsSection = new ArrayList<Food>();

        Cursor cursor = db.query(
            DBContract.FoodItems.FOODS_TABLE,
            DBContract.FoodItems.FOODS_COLUMNS,
            DBContract.FoodItems.FOODS_USER + " = ? AND "
                    + DBContract.FoodItems.FOODS_CATEGORY + " = ? AND "
                    + DBContract.FoodItems.FOODS_CONSUMATIONDATE + " = ?",
            new String[] { userId, category, date },
            null, null, null, null
        );

        if(cursor == null)
            return null;

        if(cursor.moveToFirst()) {
            do {
                long id = Long.parseLong(cursor.getString(0));
                String name = cursor.getString(1);
                String consumationDate = cursor.getString(2);
                String categoryText = cursor.getString(3);
                String subCategory = cursor.getString(4);
                String _userId = cursor.getString(5);

                listFoodItemsSection.add(0, new Food(id, name, consumationDate, categoryText, subCategory, _userId));
            } while (cursor.moveToNext());
        }

        return listFoodItemsSection;
    }

    public ArrayList<Food> getAllFoodItemsDate(String userId, String date) {
        ArrayList<Food> listFoodItemsSection = new ArrayList<Food>();

        Cursor cursor = db.query(
                DBContract.FoodItems.FOODS_TABLE,
                DBContract.FoodItems.FOODS_COLUMNS,
                DBContract.FoodItems.FOODS_USER + " = ? AND " +
                DBContract.FoodItems.FOODS_CONSUMATIONDATE + " = ?",
                new String[] { userId, date },
                null, null, null, null
        );

        if(cursor == null)
            return null;

        if(cursor.moveToFirst()) {
            do {
                long id = Long.parseLong(cursor.getString(0));
                String name = cursor.getString(1);
                String consumationDate = cursor.getString(2);
                String categoryText = cursor.getString(3);
                String subcategory = cursor.getString(4);
                String _userId = cursor.getString(5);

                listFoodItemsSection.add(0, new Food(id, name, consumationDate, categoryText, subcategory, _userId));
            } while (cursor.moveToNext());
        }

        return listFoodItemsSection;
    }

    public void removeFoodItem(long idItem) {
        db.delete(
            DBContract.FoodItems.FOODS_TABLE,
            DBContract.FoodItems.FOODS_ID + " = ?",
            new String[] { String.valueOf(idItem) }
        );
    }
}
