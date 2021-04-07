package loganalyzer.model;

import loganalyzer.dto.LogEntry;
import loganalyzer.enums.EventState;

import java.text.MessageFormat;

public class EventPair
{
    private LogEntry startEvent;
    private LogEntry finishEvent;

    public String getId() {
        // defensive in prototype stage until test generator is working well and need for this is ruled out.
        if (startEvent != null) {
            return startEvent.getId();
        } else {
            return finishEvent.getId();
        }
    }

    public long getDuration() {
        // Again, defensive against things that could happen that we don't want to bring
        // down the LogAnalyzer
        if (startEvent != null && finishEvent != null) {
            return finishEvent.getTimestamp() - startEvent.getTimestamp();
        }

        // Otherwise some logging to help trace (faulty test data) by ID
        LogEntry entry = startEvent;
        if (entry == null) {
            entry = finishEvent;
        }
        if (entry != null) {
            System.out.println("Faulty Data with ID: " + entry.getId());
        }
        return 0;
    }

    public String getHost() {
        return startEvent.getHost();
    }

    public String getType() {
        return startEvent.getType();
    }

    public void addEvent(LogEntry logEntry) {
        if (EventState.STARTED == logEntry.getEventState()) {
            startEvent = logEntry;
        } else {
            finishEvent = logEntry;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("Event Id {0} took {1} ms to execute and may require investigation",
                this.getId(), this.getDuration());
    }
}