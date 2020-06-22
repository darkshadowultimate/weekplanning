package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.api.services.calendar.Calendar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Helper extends AppCompatActivity {

    public static String[] getSUBCATEGORIES_VOICES_DB(Context context) {
        return new String[] {
            context.getString(R.string.constant_before),
            context.getString(R.string.constant_first),
            context.getString(R.string.constant_second),
            context.getString(R.string.constant_after)
        };
    }

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

    public static String getStringListLunchDinnerItemsForDB(
        String[] SUBCATEGORIES_VOICES_DB,
        ArrayList<Food> foodItems,
        String lunchOrDinner,
        Context context
    ) {
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

    public void setTimeWithTimePicker(
            Context context,
            final TimeEvent timeEvent,
            final Button timePickerStartButton,
            final Button timePickerEndButton,
            final boolean isStartTime
    ) {
        int initialHours, initialMinutes;

        if(timeEvent.isStartTimeDefined() && timeEvent.isEndTimeDefined()) {
            if(isStartTime) {
                initialHours = timeEvent.getHoursTimeStart();
                initialMinutes = timeEvent.getMinutesTimeStart();
            } else {
                initialHours = timeEvent.getHoursTimeEnd();
                initialMinutes = timeEvent.getMinutesTimeEnd();
            }
        } else {
            java.util.Calendar calendarUtil = java.util.Calendar.getInstance();

            initialHours = calendarUtil.get(java.util.Calendar.HOUR_OF_DAY);
            initialMinutes = calendarUtil.get(java.util.Calendar.MINUTE);
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if(isStartTime) {
                    timeEvent.setTimeStart(hourOfDay, minute);
                    timePickerStartButton.setText(timeEvent.getTimeStart());
                } else {
                    timeEvent.setTimeEnd(hourOfDay, minute);
                    timePickerEndButton.setText(timeEvent.getTimeEnd());
                }
            }
        }, initialHours, initialMinutes, true);

        timePickerDialog.show();
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

    public static int compareDate(String date1, String date2) {
        String[] date1Split = date1.split("-");
        String[] date2Split = date2.split("-");

        int yearDate1 = Integer.parseInt(date1Split[0]);
        int monthDate1 = Integer.parseInt(date1Split[1]);
        int dayDate1 = Integer.parseInt(date1Split[2]);
        int yearDate2 = Integer.parseInt(date2Split[0]);
        int monthDate2 = Integer.parseInt(date2Split[1]);
        int dayDate2 = Integer.parseInt(date2Split[2]);

        if(yearDate1 > yearDate2) {
            return 1;
        } else if(yearDate1 == yearDate2) {
            if(monthDate1 > monthDate2) {
                return 1;
            } else if(monthDate1 == monthDate2) {
                if(dayDate1 > dayDate2) {
                    return 1;
                } else if(dayDate1 == dayDate2) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else {
            return -1;
        }
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

    public static void handleRemoveListViewItem(
            Context context,
            final ArrayList<Food> generalArrayList,
            final ArrayList<Food> newItemsArrayList,
            final ArrayList<Food> itemsToDeleteArrayList,
            final ArrayAdapter<Food> adapter,
            final int indexItemToRemove
    ) {
        new AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.warning_remove_list_item_element_title))
            .setMessage(
                    generalArrayList.size() == 1
                            ? context.getString(R.string.warning_remove_list_item_element_last)
                            : context.getString(R.string.warning_remove_list_item_element_message)
            )
            .setPositiveButton(context.getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Food item = generalArrayList.get(indexItemToRemove);

                    if(!newItemsArrayList.contains(item)) {
                        itemsToDeleteArrayList.add(item);
                    } else {
                        newItemsArrayList.remove(item);
                    }

                    generalArrayList.remove(indexItemToRemove);
                    adapter.notifyDataSetChanged();
                }
            })
            .setNegativeButton(context.getString(R.string.button_no), null)
            .show();
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

    public static void deleteMealCardImmediatly(
        final Activity activity,
        final Context context,
        final DBAdapter localDB,
        final Calendar service,
        final ImageView previewImage,
        final String typeOfMeal,
        final String dateSelected,
        final String uid,
        final String weekPlanningCalendarId,
        final String idGoogleCalendarEvent,
        final int sizeGeneralArrayList,
        final int sizeNewItemslArrayList,
        final int sizeItemsToDeleteArrayList
    ) {
        final HandlePictureFromCamera handlePictureFromCamera = new HandlePictureFromCamera();

        new AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.warning_delete_card_meal_immediatly_title))
            .setMessage(context.getString(R.string.warning_delete_card_meal_immediatly_message))
            .setPositiveButton(context.getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(
                        sizeGeneralArrayList > 0 &&
                        (sizeGeneralArrayList != sizeNewItemslArrayList && sizeItemsToDeleteArrayList == 0)
                    ) {
                        localDB.removeMeal(dateSelected, typeOfMeal);
                        handlePictureFromCamera.handleDeleteImage(
                                false,
                                uid,
                                dateSelected,
                                typeOfMeal,
                                previewImage,
                                context
                        );
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    GoogleCalendarHelper.deleteCalendarEvent(
                                            service,
                                            weekPlanningCalendarId,
                                            idGoogleCalendarEvent
                                    );

                                    localDB.removeGoogleCalendarEvent(dateSelected, typeOfMeal);
                                } catch(IOException exc) {
                                    Log.e("ERROR DELETING EVENT", "EVENT NOT DELETED", exc);
                                }
                            }
                        }).start();
                    }

                    activity.setResult(Activity.RESULT_OK, new Intent());
                    activity.finish();
                }
            })
            .setNegativeButton(context.getString(R.string.button_no), null)
            .show();
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
}
