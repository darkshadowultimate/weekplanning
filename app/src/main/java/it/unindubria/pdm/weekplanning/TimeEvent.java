package it.unindubria.pdm.weekplanning;

class TimeEvent {
    private String timeStart;
    private String timeEnd;
    private int hoursTimeStart;
    private int minutesTimeStart;
    private int hoursTimeEnd;
    private int minutesTimeEnd;

    public TimeEvent() {
        this.timeStart = null;
        this.timeEnd = null;
    }

    public TimeEvent(String timeStart, String timeEnd) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;

        this.hoursTimeStart = getHoursFromTimeString(timeStart);
        this.minutesTimeStart = getMinutesFromTimeString(timeStart);
        this.hoursTimeEnd = getHoursFromTimeString(timeEnd);
        this.minutesTimeEnd = getMinutesFromTimeString(timeEnd);
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
        return Integer.parseInt(timeString.split(":")[0]);
    }

    private static int getMinutesFromTimeString(String timeString) {
        return Integer.parseInt(timeString.split(":")[0]);
    }

    private String getTimeAsString(int hours, int minutes) {
        return (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":00";
    }

    public void setTimeStart(int hours, int minutes) {
        this.hoursTimeStart = hours;
        this.minutesTimeStart = minutes;
        this.timeStart = getTimeAsString(hours, minutes);
    }

    public void setTimeEnd(int hours, int minutes) {
        this.hoursTimeEnd = hours;
        this.minutesTimeEnd = minutes;
        this.timeEnd = getTimeAsString(hours, minutes);
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
