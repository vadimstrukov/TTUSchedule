package ee.ttu.schedule.model;

import android.database.Cursor;

import java.io.Serializable;

public class Event implements Serializable {

    private int ID;
    private Long dateStart;
    private Long dateEnd;
    private String description;
    private String location;
    private String summary;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Long getDateStart() {
        return dateStart;
    }

    public void setDateStart(Long dateStart) {
        this.dateStart = dateStart;
    }

    public Long getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Long dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public static Event generateFromCursor(Cursor cursor){
        final Event event = new Event();
        event.setID(cursor.getInt(0));
        event.setDateStart(cursor.getLong(0));
        event.setDateEnd(cursor.getLong(0));
        event.setDescription(cursor.getString(0));
        event.setLocation(cursor.getString(0));
        event.setSummary(cursor.getString(0));
        return event;
    }

}
