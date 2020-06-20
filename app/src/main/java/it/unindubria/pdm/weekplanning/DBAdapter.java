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

    public long insertNewUserCalendar(String uid, String calendarId) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DBContract.UserCalendars.USERCALENDARS_UID, uid);
        contentValues.put(DBContract.UserCalendars.USERCALENDARS_CALENDARID, calendarId);

        long idRecordUserCalendar = db.insert(
                DBContract.UserCalendars.USERCALENDARS_TABLE,
                null,
                contentValues);

        return idRecordUserCalendar;
    }

    public void insertGoogleCalendarEvent(GoogleCalendarEvent googleCalendarEvent) {
        long idFoodItem = db.insert(
            DBContract.CalendarEvents.CALENDAREVENTS_TABLE,
            null,
            googleCalendarEvent.getAsContentValue()
        );
    }

    public void updateGoogleCalendarEvent(GoogleCalendarEvent googleCalendarEvent) {
        db.update(
            DBContract.CalendarEvents.CALENDAREVENTS_TABLE,
            googleCalendarEvent.getAsContentValue(),
            DBContract.CalendarEvents.CALENDAREVENTS_DATE + " = ? AND " +
            DBContract.CalendarEvents.CALENDAREVENTS_CATEGORY_MEAL + " = ?",
            new String[] { googleCalendarEvent.getDateEvent(), googleCalendarEvent.getCategoryMeal() }
        );
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

    public String getCalendarId(String uid) {
        Cursor cursor = db.query(
                DBContract.UserCalendars.USERCALENDARS_TABLE,
                DBContract.UserCalendars.USERCALENDARS_COLUMNS,
                DBContract.UserCalendars.USERCALENDARS_UID + " = ?",
                new String[] { uid },
                null, null, null, null
        );

        if(cursor == null || cursor.getCount() == 0) {
            return null;
        } else {
            cursor.moveToFirst();
            return cursor.getString(1);
        }
    }

    public GoogleCalendarEvent getGoogleCalendarEvent(String dateEvent, String category) {
        Cursor cursor = db.query(
                DBContract.CalendarEvents.CALENDAREVENTS_TABLE,
                DBContract.CalendarEvents.CALENDAREVENTS_COLUMNS,
                DBContract.CalendarEvents.CALENDAREVENTS_DATE + " = ? AND " +
                DBContract.CalendarEvents.CALENDAREVENTS_CATEGORY_MEAL + " = ?",
                new String[] { dateEvent, category },
                null, null, null, null
        );

        if(cursor == null || cursor.getCount() == 0) {
            return null;
        } else {
            cursor.moveToFirst();

            String id = cursor.getString(0);
            String timeStart = cursor.getString(1);
            String timeEnd = cursor.getString(2);
            String description = cursor.getString(3);
            String date = cursor.getString(4);
            String categoryMeal = cursor.getString(5);



            return new GoogleCalendarEvent(id, timeStart, timeEnd, description, date, categoryMeal);
        }
    }

    public void removeFoodItem(long idItem) {
        db.delete(
            DBContract.FoodItems.FOODS_TABLE,
            DBContract.FoodItems.FOODS_ID + " = ?",
            new String[] { String.valueOf(idItem) }
        );
    }

    public void removeGoogleCalendarEvent(String dateEvent, String category) {
        db.delete(
            DBContract.CalendarEvents.CALENDAREVENTS_TABLE,
            DBContract.CalendarEvents.CALENDAREVENTS_DATE + " = ? AND " +
            DBContract.CalendarEvents.CALENDAREVENTS_CATEGORY_MEAL + " = ?",
            new String[] { dateEvent, category }
        );
    }
}
