package it.unindubria.pdm.weekplanning;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.api.services.calendar.Calendar;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Helper extends AppCompatActivity {

    public static final String[] SUBCATEGORIES_VOICES_DB = { "before", "first", "second", "after" };

    public Intent changeActivity(Context context, Class classToLoad) {
        Intent intent = new Intent(context, classToLoad);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        return intent;
    }

    public boolean isThereAtLeastACharacter(String stringToCheck) {
        for(int i = 0; i < stringToCheck.length(); i++) {
            if(Character.isLetter(stringToCheck.charAt(i)))
                return true;
        }
        return false;
    }

    public String getStringListBreakfastItem(ArrayList<Food> foodItems) {
        String allItems = "";

        for(Food item: foodItems) {
            if(item.getCategory().equals("breakfast")) {
                allItems += "- " + item.getName() + "\n";
            }
        }

        return allItems;
    }

    public String getStringListBreakfastItemForDB(ArrayList<Food> foodItems) {
        String allItems = "";

        for(Food item: foodItems) {
            if(item.getCategory().equals("breakfast")) {
                allItems += item.getName() + "//";
            }
        }

        return allItems;
    }

    public ArrayList<String> convertListBreakfastItemsDBToArrayList(String items) {
        ArrayList<String> arrayListItems = new ArrayList<String>();
        String[] arrayItems = items.split("//");

        for(String item: arrayItems) {
            arrayListItems.add(item);
        }

        return arrayListItems;
    }

    public static String getStringListLunchDinnerItemsForDB(ArrayList<Food> foodItems, String lunchOrDinner, Context context) {
        String allItems = "";

        for (String subcategory : SUBCATEGORIES_VOICES_DB) {
            String itemsOfCurrentSubCategory = "";

            for (Food item : foodItems) {
                if (item.getCategory().equals(lunchOrDinner) && item.getSubcategory().equals(subcategory)) {
                    itemsOfCurrentSubCategory += "\t- " + item.getName() + "\n";
                }
            }
            if(!itemsOfCurrentSubCategory.isEmpty()) {
                int codeSubcategoryTranslated = context
                    .getResources().getIdentifier(subcategory, "string", context.getPackageName());
                allItems += context.getString(codeSubcategoryTranslated) + ":\n" + itemsOfCurrentSubCategory;
            }
        }

        return allItems;
    }

    public boolean areListItemsDifferent(ArrayList<String> listItemsString, ArrayList<Food> listItemsFood) {
        for(int i = 0; i < listItemsFood.size(); i++) {
            if(listItemsFood.get(i).getName().equals(listItemsString.get(0))) {
                return true;
            }
        }
        return false;
    }

    public int getHoursFromString(String time) {
        return Integer.parseInt(time.split(":")[0]);
    }

    public int getMinutesFromString(String time) {
        return Integer.parseInt(time.split(":")[1]);
    }

    public boolean isEndTimeBiggerThanStartTime(String startTime, String endTime) {
        if(getHoursFromString(endTime) > getHoursFromString(startTime)) {
            return true;
        } else if(getHoursFromString(endTime) == getHoursFromString(startTime)) {
            if(getMinutesFromString(endTime) > getMinutesFromString(startTime)) {
                return true;
            }
        }
        return false;
    }

    public void createDirectoryStructure(String uid, String date, String category) {
        String
            basePath = "/WeekPlanning",
            uidPath = basePath + "/" + uid,
            datePath = uidPath + "/" + date,
            categoryPath = datePath + "/" + category;

        createNewDirectory(basePath);
        createNewDirectory(uidPath);
        createNewDirectory(datePath);
        createNewDirectory(categoryPath);
    }

    public void createNewDirectory(String restOfPath) {
        String absolutePathAppDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + restOfPath;
        Path path = Paths.get(absolutePathAppDirectory);

        if(Files.notExists(path)) {
            //dispayWithLog("CREATING DIRECTORY", "DIRECTORY DOESN'T EXISTS");
            File dir = new File(absolutePathAppDirectory);
            try{
                dir.mkdir();
            }catch(Exception e){
                e.printStackTrace();
            }
        } else {
            //dispayWithLog("CREATING DIRECTORY", "DIRECTORY ALREADY EXISTS");
        }
    }

    public String getStringDate(int day, int month, int year) {
        return year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;
    }

    public String getStringTime(int hours, int mins) {
        return (hours < 10 ? "0" : "") + hours + ":" + (mins < 10 ? "0" : "") + mins + ":00";
    }

    public int getCameraPermissionCode(Context context) {
        return Integer.parseInt(context.getString(R.string.constant_permission_code_camera));
    }

    public int getStoragePermissionCode(Context context) {
        return Integer.parseInt(context.getString(R.string.constant_permission_code_readWriteStorage));
    }

    public int getBreakfastCodeStartActivity(Context context) {
        return Integer.parseInt(context.getString(R.string.constant_start_activity_code_breakfast));
    }

    public int getLunchDinnerCodeStartActivity(Context context) {
        return Integer.parseInt(context.getString(R.string.constant_start_activity_code_lunchDinner));
    }

    public int getGoogleCalendarCodeStartActivity(Context context) {
        return Integer.parseInt(context.getString(R.string.constant_start_activity_code_googleCalPerm));
    }

    public int getTakePictureCodeStartActivity(Context context) {
        return Integer.parseInt(context.getString(R.string.constant_start_activity_code_takePicture));
    }

    public static void addAllFoodItemsToDBWhichWereAdded(DBAdapter localDB, ArrayList<Food> itemsToAdd) {
        for(Food foodItem: itemsToAdd) {
            localDB.insert(foodItem);
        }
    }

    public static void deleteAllFoodItemsFromDBWhichWereRemoved(DBAdapter localDB, ArrayList<Food> itemsToRemove) {
        for(Food foodItem: itemsToRemove) {
            localDB.removeFoodItem(foodItem.getId());
        }
    }

    public static void deleteGoogleCalendarEvent(
            final Calendar service,
            final DBAdapter localDB,
            final GoogleCalendarEvent googleCalendarEvent,
            final String weekPlanningCalendarId,
            final String idGoogleCalendarEvent,
            final String dateSelected,
            final String category
    ) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("DELETE EVENT", "INSIDE METHOD TO DELETE EVENT");
                if(googleCalendarEvent != null) {
                    Log.e("DELETE EVENT", "DELETE EVENT FROM LOCAL DB");
                    localDB.removeGoogleCalendarEvent(dateSelected, category);

                    try {
                        Log.e("DELETE EVENT", "DELETE EVENT FROM GOOGLE CALENDAR API");
                        GoogleCalendarHelper.deleteCalendarEvent(service, weekPlanningCalendarId, idGoogleCalendarEvent);
                    } catch(Exception exc) {
                        Log.e("CALENDAR EVENT DELETE", "THE EVENT DOESN'T EXISTS");
                    }
                }
            }
        }).start();
    }

    public void displayWithDialog(Context context, int title, int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder
        .setMessage(message)
        .setTitle(title)
        .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public void displayWithDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder
            .setMessage(message)
            .setTitle(title)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public void displayWithToast(Context context, int message) {
        Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    public void displayWithToast(Context context, String message) {
        Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    public void dispayWithLog(String tag, String message) {
        Log.d(tag, message);
    }
}
