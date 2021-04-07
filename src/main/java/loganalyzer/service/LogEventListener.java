package loganalyzer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import loganalyzer.dto.LogEntry;
import loganalyzer.model.EventPair;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class LogEventListener extends TailerListenerAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(LogEventListener.class);

    private static final int BATCH_SIZE = 200;
    private static final int THROTTLE_WAIT_TIME_IN_MILLIS = 60;
    private long eventCount = 0;

    private Map<String, LogEntry> logEventHolder;
    private List<EventPair> completeEvents;

    private Tailer tailer;
    private final Gson jsonToObjectParser;

    public LogEventListener() {
        if (logger.isDebugEnabled()) {
            logger.debug("LogEventListener(batch={},wait_time={})", BATCH_SIZE, THROTTLE_WAIT_TIME_IN_MILLIS);
        }
        logEventHolder = new HashMap<>();
        completeEvents = new LinkedList<>();
        this.jsonToObjectParser = new GsonBuilder().create();
    }

    @Override
    public void init(final Tailer tailer) {
        this.tailer = tailer;
    }

    public void stop() {
        this.tailer.stop();
    }

    public synchronized List<EventPair> getAndResetEventPairs() {
        List<EventPair> batch = this.completeEvents;
        this.completeEvents = new LinkedList<>();
        return batch;
    }

    @Override
    public void fileRotated() {
        System.out.println("Rotated");
    }

    @Override
    public void handle(String line) {
        if (StringUtils.isBlank(line)) {
            logger.warn("Ignoring blank line: {}", line);
            return;
        }

        eventCount++;

        if (logger.isDebugEnabled()) {
            logger.debug("Handle event " + eventCount);
        }

        if (completeEvents.size() >= BATCH_SIZE) {
            logger.debug("Pausing to allow Events to be persisted to Database");
            try {
                Thread.sleep(THROTTLE_WAIT_TIME_IN_MILLIS);
            } catch (InterruptedException ex) {
                logger.error("Unexpected Exception: ", ex);
            }
        }

        LogEntry newLogEntry = this.jsonToObjectParser.fromJson(line, LogEntry.class);
        LogEntry logEntry = logEventHolder.remove(newLogEntry.getId());
        if (logEntry == null) {
            logEventHolder.put(newLogEntry.getId(), newLogEntry);
        } else {
            storeCompletedEvent(logEntry, newLogEntry);
        }
    }

    private synchronized void storeCompletedEvent(LogEntry existingLogEntry, LogEntry newLogEntry) {
        EventPair eventPair = new EventPair();
        eventPair.addEvent(existingLogEntry);
        eventPair.addEvent(newLogEntry);
        completeEvents.add(eventPair);
        if (eventPair.getDuration() > 4) {
            logger.warn("Detected long running event: {}", eventPair);
        }
    }

    @Override
    public void handle(Exception ex) {
        logger.error("ExceptionNotified asynchronously: ",ex);
    }

    public void tailFile(String filename) {
        File file = new File(filename);
        Tailer.create(file, this);
    }
}