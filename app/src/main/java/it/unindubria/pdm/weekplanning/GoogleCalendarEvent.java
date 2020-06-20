package it.unindubria.pdm.weekplanning;

import android.content.ContentValues;

class GoogleCalendarEvent {
    private String id;
    private String timeStart;
    private String timeEnd;
    private String description;
    private String dateEvent;
    private String categoryMeal;

    public GoogleCalendarEvent(
        String id,
        String timeStart,
        String timeEnd,
        String description,
        String dateEvent,
        String categoryMeal
    ) {
        this.id = id;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.description = description;
        this.dateEvent = dateEvent;
        this.categoryMeal = categoryMeal;
    }

    public String getId() {
        return id;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public String getDescription() {
        return description;
    }

    public String getDateEvent() {
        return dateEvent;
    }

    public String getCategoryMeal() {
        return categoryMeal;
    }

    public ContentValues getAsContentValue() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DBContract.CalendarEvents.CALENDAREVENTS_ID, this.id);
        contentValues.put(DBContract.CalendarEvents.CALENDAREVENTS_TIMESTART, this.timeStart);
        contentValues.put(DBContract.CalendarEvents.CALENDAREVENTS_TIMEEND, this.timeEnd);
        contentValues.put(DBContract.CalendarEvents.CALENDAREVENTS_DESCRIPTION, this.description);
        contentValues.put(DBContract.CalendarEvents.CALENDAREVENTS_DATE, this.dateEvent);
        contentValues.put(DBContract.CalendarEvents.CALENDAREVENTS_CATEGORY_MEAL, this.categoryMeal);

        return contentValues;
    }

    public String toString() {
        return
            "Id: " + this.id +
            "\nTimeStart: " + this.timeStart +
            "\nTimeEnd: " + this.timeEnd +
            "\nDescription: " + this.description +
            "\nDate: " + this.dateEvent +
            "\nCategory: " + this.categoryMeal;
    }
}
