package it.unindubria.pdm.weekplanning;

import android.util.Log;

import java.util.Calendar;

class TimeEvent {
    // The time which indicates when the Google Calendar event starts (String)
    // Format => hh:mm:ss
    private String timeStart;
    // The time which indicates when the Google Calendar event ends (String)
    // Format => hh:mm:ss
    private String timeEnd;
    private int hoursTimeStart;
    private int minutesTimeStart;
    private int hoursTimeEnd;
    private int minutesTimeEnd;
    // Indicated if the TimePicker was used to set the time (timeStart, timeEnd or both)
    private boolean timeChanged;

    public TimeEvent() {
        this.timeStart = null;
        this.timeEnd = null;
        this.timeChanged = false;
    }

    public TimeEvent(String timeStart, String timeEnd) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;

        this.hoursTimeStart = getHoursFromTimeString(timeStart);
        this.minutesTimeStart = getMinutesFromTimeString(timeStart);
        this.hoursTimeEnd = getHoursFromTimeString(timeEnd);
        this.minutesTimeEnd = getMinutesFromTimeString(timeEnd);

        this.timeChanged = false;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public int getHoursTimeStart() {
        return hoursTimeStart;
    }

    public int getMinutesTimeStart() {
        return minutesTimeStart;
    }

    public int getHoursTimeEnd() {
        return hoursTimeEnd;
    }

    public int getMinutesTimeEnd() {
        return minutesTimeEnd;
    }

    private static int getHoursFromTimeString(String timeString) {
        // timeString format => hh:mm:ss
        // after split => [hh, mm, ss]
        return Integer.parseInt(timeString.split(":")[0]);
    }

    private static int getMinutesFromTimeString(String timeString) {
        // timeString format => hh:mm:ss
        // after split => [hh, mm, ss]
        return Integer.parseInt(timeString.split(":")[1]);
    }

    private String getTimeAsString(int hours, int minutes) {
        // convert to hh:mm:ss (seconds always 00)
        // numbers less than 10 must be preceded by a 0 (zero)
        return (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":00";
    }

    public void setTimeStart(int hours, int minutes) {
        this.hoursTimeStart = hours;
        this.minutesTimeStart = minutes;
        this.timeStart = getTimeAsString(hours, minutes);
        // the time was changes
        this.timeChanged = true;
    }

    public void setTimeEnd(int hours, int minutes) {
        this.hoursTimeEnd = hours;
        this.minutesTimeEnd = minutes;
        this.timeEnd = getTimeAsString(hours, minutes);
        // the time was changes
        this.timeChanged = true;
    }

    public boolean didTimeChanged() {
        return this.timeChanged;
    }

    public boolean isStartTimeDefined() {
        return this.timeStart != null;
    }

    public boolean isEndTimeDefined() {
        return this.timeEnd != null;
    }

    public boolean isTimeEventDefined() {
        return isStartTimeDefined() && isEndTimeDefined();
    }

    private boolean isFirstTimeLessThanSecondTime(int hours1, int minutes1, int hours2, int minutes2) {
        if(hours1 < hours2) {
            return true;
        } else if(hours1 == hours2) {
            if(
                minutes2 >= 20 &&
                minutes1 <= (minutes2 - 20)
            ) {
                return true;
            }
        }
        return false;
    }

    // Used to determine if the timeStart and endTime was selected correctly.
    // There's has to be at least a difference of 20 minutes between the timeStart and the timeEnd.
    // There's has to be at least a difference of 20 minutes between the currentTime and the timeEnd (if the event's date is the same of the current one).
    // If the event's date is in the past, then you cannot insert the event.
    public boolean isTimeEventEndInTheFuture(String dateEvent) {
        Calendar currentTime = Calendar.getInstance();
        // get 24 hours format
        int currentTimeHours = currentTime.get(Calendar.HOUR_OF_DAY);
        int currentTimeMinutes = currentTime.get(Calendar.MINUTE);
        int currentDateYear = currentTime.get(Calendar.YEAR);
        int currentDateMonth = currentTime.get(Calendar.MONTH);
        int currentDateDay = currentTime.get(Calendar.DAY_OF_MONTH);

        Log.e("START TIME & END TIME =======> ", this.timeStart + " ----- " + this.timeEnd);
        Log.e("START TIME =======> ", this.hoursTimeStart + " -- " + this.minutesTimeStart);
        Log.e("END TIME =======> ", this.hoursTimeEnd + " -- " + this.minutesTimeEnd);

        String currentDateString = currentDateYear + "-" + (currentDateMonth + 1 < 10 ? "0" : "") + (currentDateMonth + 1) + "-" + (currentDateDay < 10 ? "0" : "") + currentDateDay;

        int dateEventBiggerThanCurrentDate = Helper.compareDate(dateEvent, currentDateString);
        boolean isStartTimeLessThanEndTimeEnough = isFirstTimeLessThanSecondTime(
                this.hoursTimeStart,
                this.minutesTimeStart,
                this.hoursTimeEnd,
                this.minutesTimeEnd
        );

        if(dateEventBiggerThanCurrentDate == 1) {
            // there's no need to check the currentTime too
            return isStartTimeLessThanEndTimeEnough;
        } else if(dateEventBiggerThanCurrentDate == 0) {
            return
                // check if the difference between currentTime and endTime is equals or more than 20 min
                isFirstTimeLessThanSecondTime(
                    currentTimeHours,
                    currentTimeMinutes,
                    this.hoursTimeEnd,
                    this.minutesTimeEnd
                ) &&
                // check if the difference between startTime and endTime is equals or more than 20 min
                isStartTimeLessThanEndTimeEnough;
        } else {
            return false;
        }
    }

    public boolean isTimeStartLessThanTimeEnd() {
        if(this.hoursTimeEnd > hoursTimeStart) {
            return true;
        } else if(hoursTimeEnd == hoursTimeStart) {
            if(minutesTimeEnd > minutesTimeStart) {
                return true;
            }
        }
        return false;
    }
}
