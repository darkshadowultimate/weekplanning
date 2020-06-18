package it.unindubria.pdm.weekplanning;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.util.Arrays;

public class GoogleCalendarHelper {

    private static final String[] SCOPES = {
            CalendarScopes.CALENDAR_READONLY,
            CalendarScopes.CALENDAR
    };
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final static HttpTransport TRANSPORT = AndroidHttp.newCompatibleTransport();
    private final static String APPLICATION_NAME = "WeekPlanning";

    public static Calendar getCalendarBuilderInstance(Context context, String emailAccount) {
        GoogleAccountCredential credential = GoogleAccountCredential
            .usingOAuth2(
                context,
                Arrays.asList(SCOPES)
            );

        credential.setSelectedAccountName(emailAccount);

        Calendar service = new Calendar.Builder(TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .setHttpRequestInitializer(credential)
            .build();

        return service;
    }

    public static boolean createNewCalendar(Calendar service) {
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary("WeekPlanningCalendar");
        calendar.setTimeZone("Europe/Rome");

        try {
            service.calendars().insert(calendar).execute();
            return true;
        } catch(IOException exc) {
            return false;
        }
    }

    public static void createNewEvent(
            Calendar service,
            String summary,
            String description,
            String date,
            String timeStart,
            String timeEnd
    ) throws IOException {
        Event event = new Event()
                .setSummary(summary)
                .setDescription(description);

        DateTime startDateTime = new DateTime(date + "T" + timeStart + "+02:00");

        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Europe/Rome");
        event.setStart(start);

        DateTime endDateTime = new DateTime(date + "T" + timeEnd + "+02:00");

        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Europe/Rome");
        event.setEnd(end);

        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(30)
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        String calendarId = "primary";
        event = service.events().insert(calendarId, event).execute();
    }
}
