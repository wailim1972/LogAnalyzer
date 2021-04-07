package loganalyzer.entity;

import loganalyzer.model.EventPair;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="LOG_EVENT")
public class Event
{
    @Column(name="ID")
    @Id
    String id;

    @Column(name="DURATION")
    Long duration;

    @Column(name="ALERT")
    Boolean alert;

    @Column(name="TYPE")
    String type;

    @Column(name="HOST")
    String host;

    public Event() {
        // Public default ctor for JPA Layer
    }

    // Convenience method
    public static Event fromEventPair(EventPair eventPair) {
        Event event = new Event();
        event.id = eventPair.getId();
        event.duration = eventPair.getDuration();
        event.alert = event.duration > 4;
        event.type = eventPair.getType();
        event.host = eventPair.getHost();
        return event;
    }

    public String getId() {
        return id;
    }

    public Long getDuration() {
        return duration;
    }

    public Boolean getAlert() {
        return alert;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }
}