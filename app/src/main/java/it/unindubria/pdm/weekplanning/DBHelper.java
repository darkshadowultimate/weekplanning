package it.unindubria.pdm.weekplanning;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper sInstance;

    public static synchronized DBHelper getInstance(Context context) {
        if(sInstance == null)
            sInstance = new DBHelper(context.getApplicationContext());
        return sInstance;
    }

    public DBHelper(Context context) {
        super(context, DBContract.DB_NAME, null, DBContract.DB_VERSION);
    }

    private String CREATE_FOODS_TABLE =
        "CREATE TABLE " + DBContract.FoodItems.FOODS_TABLE + " (" +
        DBContract.FoodItems.FOODS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        DBContract.FoodItems.FOODS_NAME + " TEXT, " +
        DBContract.FoodItems.FOODS_CONSUMATIONDATE + " TEXT, " +
        DBContract.FoodItems.FOODS_CATEGORY + " TEXT, " +
        DBContract.FoodItems.FOODS_SUBCATEGORY + " TEXT, " +
        DBContract.FoodItems.FOODS_USER + " TEXT )";

    private String CREATE_USERCALENDARS_TABLE =
        "CREATE TABLE " + DBContract.UserCalendars.USERCALENDARS_TABLE + " (" +
            DBContract.UserCalendars.USERCALENDARS_UID + " TEXT PRIMARY KEY, " +
            DBContract.UserCalendars.USERCALENDARS_CALENDARID + " TEXT ) ";

    private String CREATE_CALENDAREVENTS_TABLE =
        "CREATE TABLE " + DBContract.CalendarEvents.CALENDAREVENTS_TABLE + " (" +
            DBContract.CalendarEvents.CALENDAREVENTS_ID + " TEXT PRIMARY KEY, " +
            DBContract.CalendarEvents.CALENDAREVENTS_TIMESTART + " TEXT, " +
            DBContract.CalendarEvents.CALENDAREVENTS_TIMEEND + " TEXT, " +
            DBContract.CalendarEvents.CALENDAREVENTS_DESCRIPTION + " TEXT, " +
            DBContract.CalendarEvents.CALENDAREVENTS_DATE + " TEXT, " +
            DBContract.CalendarEvents.CALENDAREVENTS_CATEGORY_MEAL + " TEXT )";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FOODS_TABLE);
        db.execSQL(CREATE_USERCALENDARS_TABLE);
        db.execSQL(CREATE_CALENDAREVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DBContract.FoodItems.FOODS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DBContract.UserCalendars.USERCALENDARS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DBContract.CalendarEvents.CALENDAREVENTS_TABLE);

        this.onCreate(db);
    }


}
