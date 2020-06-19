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
        static final String FOODS_SUBCATEGORY = "subcategory";
         static final String FOODS_USER = "userId";

         static final String[] FOODS_COLUMNS = {
            FOODS_ID, FOODS_NAME, FOODS_CONSUMATIONDATE, FOODS_CATEGORY, FOODS_SUBCATEGORY, FOODS_USER
        };
    }

    static abstract class UserCalendars implements BaseColumns {
        static final String USERCALENDARS_TABLE = "usercalendars";

        static final String USERCALENDARS_UID = "uid";
        static final String USERCALENDARS_CALENDARID = "calendarId";

        static final String[] USERCALENDARS_COLUMNS = {
                USERCALENDARS_UID, USERCALENDARS_CALENDARID
        };
    }
}
