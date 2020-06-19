package it.unindubria.pdm.weekplanning;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class Helper extends AppCompatActivity {

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
        LocalDate localDate = LocalDate.of(year, month, day);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ITALIAN);
        return formatter.format(localDate);
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
