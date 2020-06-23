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

    public static String createNewCalendar(
        Calendar service,
        String uid,
        String description
    ) throws IOException {
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary("WeekPlanningCalendar");
        calendar.setTimeZone("Europe/Rome");
        calendar.setDescription(description);

        return service.calendars().insert(calendar).execute().getId();
    }

    private static EventDateTime convertToEventDateTime(String date, String time) {
        DateTime startDateTime = new DateTime(date + "T" + time + "+02:00");

        return new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Europe/Rome");
    }

    public static String createNewEvent(
            Calendar service,
            String calendarId,
            String summary,
            String description,
            String date,    // yyyy-mm-gg
            String timeStart,   // hh:mm:ss
            String timeEnd  // hh:mm:ss
    ) throws IOException {
        Event event = new Event()
                .setSummary(summary)
                .setDescription(description);

        event.setStart(convertToEventDateTime(date, timeStart));

        event.setEnd(convertToEventDateTime(date, timeEnd));

        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(30)
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        return service.events().insert(calendarId, event).execute().getId();
    }

    public static void updateCalendarEvent(
        Calendar service,
        String idCalendar,
        String idGoogleCalendarEvent,
        String date,
        String timeStart,
        String timeEnd,
        String summary,
        String description
    ) throws IOException {
        Event event = service.events().get(idCalendar, idGoogleCalendarEvent).execute();

        event.setStart(convertToEventDateTime(date, timeStart));
        event.setEnd(convertToEventDateTime(date, timeEnd));
        event.setSummary(summary);
        event.setDescription(description);

        service.events().patch(idCalendar, event.getId(), event).execute();
    }

    public static void deleteCalendarEvent(
        Calendar service,
        String idCalendar,
        String idEvent
    ) throws IOException {
        service.events().delete(idCalendar, idEvent).execute();
    }
}
