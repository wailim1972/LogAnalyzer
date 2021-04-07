package loganalyzer.dto;

import com.google.gson.annotations.SerializedName;
import loganalyzer.enums.EventState;

public class LogEntry {
    String id;
    @SerializedName("state")
    EventState eventState;
    long timestamp;
    String type;
    String host;

    public String getId() {
        return id;
    }

    public EventState getEventState() {
        return eventState;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }
}