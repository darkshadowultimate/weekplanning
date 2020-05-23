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

    String CREATE_FOODS_TABLE =
        "CREATE TABLE " + DBContract.FoodItems.FOODS_TABLE + " (" +
        DBContract.FoodItems.FOODS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        DBContract.FoodItems.FOODS_NAME + " TEXT, " +
        DBContract.FoodItems.FOODS_CONSUMATIONDATE + " TEXT, " +
        DBContract.FoodItems.FOODS_CATEGORY + " TEXT, " +
        DBContract.FoodItems.FOODS_USER + " TEXT )";

    String CREATE_CATEGORIES_TABLE =
        "CREATE TABLE " + DBContract.CategoryItems.CATEGORIES_TABLE + " (" +
        DBContract.CategoryItems.CATEGORIES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        DBContract.CategoryItems.CATEGORIES_NAME + " TEXT )";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FOODS_TABLE);
        db.execSQL(CREATE_CATEGORIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DBContract.FoodItems.FOODS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DBContract.CategoryItems.CATEGORIES_TABLE);

        this.onCreate(db);
    }


}
