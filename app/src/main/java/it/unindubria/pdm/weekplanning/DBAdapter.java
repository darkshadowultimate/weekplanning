package it.unindubria.pdm.weekplanning;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

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

    public DBAdapter open() throws SQLException {
        db = dbHelper.getWritableDatabase();
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
}
