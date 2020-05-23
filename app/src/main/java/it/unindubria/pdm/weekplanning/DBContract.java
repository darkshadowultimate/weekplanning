package it.unindubria.pdm.weekplanning;

import android.provider.BaseColumns;

public class DBContract {
    static final int DB_VERSION = 1;
    static final String DB_NAME = "WeekPlanning";

     DBContract() {}

    static abstract class FoodItems implements BaseColumns {
         static final String FOODS_TABLE = "foods";

         static final String FOODS_ID = "id";
         static final String FOODS_NAME = "name";
         static final String FOODS_CONSUMATIONDATE = "consumationDate";
         static final String FOODS_CATEGORY = "category";
         static final String FOODS_USER = "user";

         static final String[] FOODS_COLUMNS = {
            FOODS_ID, FOODS_NAME, FOODS_CONSUMATIONDATE, FOODS_CATEGORY, FOODS_USER
        };
    }

    static abstract class CategoryItems implements BaseColumns {
         static final String CATEGORIES_TABLE = "categories";

         static final String CATEGORIES_ID = "id";
         static final String CATEGORIES_NAME = "name";

         static final String[] CATEGORIES_COLUMNS = {
            CATEGORIES_ID, CATEGORIES_NAME
        };
    }
}
