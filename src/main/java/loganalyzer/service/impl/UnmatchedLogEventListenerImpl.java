package loganalyzer.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import loganalyzer.dto.LogEntry;
import loganalyzer.model.EventPair;
import loganalyzer.service.MatchedLogEventProcessor;
import loganalyzer.service.UnmatchedLogEventHandler;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
public class UnmatchedLogEventListenerImpl extends TailerListenerAdapter
        implements UnmatchedLogEventHandler, CommandLineRunner
{
    private static final Logger logger = LoggerFactory.getLogger(UnmatchedLogEventListenerImpl.class);
    private Tailer tailer;
    private final Gson jsonToObjectParser;
    private Map<String, LogEntry> unmatchedLogEvents;
    private long eventCount = 0;
    private volatile boolean paused = false;

    @Autowired
    private MatchedLogEventProcessor matchedLogEventProcessor;

    public UnmatchedLogEventListenerImpl() {
        logger.info("UnmatchedLogEventListenerImpl({})", this);
        unmatchedLogEvents = new HashMap<>();
        this.jsonToObjectParser = new GsonBuilder().create();
    }

    @Override
    public synchronized void pause() {
        paused = true;
    }

    @Override
    public synchronized void resume() {
        paused = false;
        // Wake up the tailer thread
        this.notify();
    }

    @Override
    public void run(String... args) throws IllegalArgumentException {
        if (args.length != 1 || StringUtils.isBlank(args[0])) {
            String msg = "Usage: gradlew -Pparams=<filepath> bootRun";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        File file = new File(args[0]);
        Tailer.create(file, this);

        // Keep the outer process from exiting
        while(true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void init(final Tailer tailer) {
        this.tailer = tailer;
    }

    @Override
    public void fileRotated() {
        logger.info("Underlying file rotation detected");
    }

    @Override
    public void handle(String line) {
        if (this.paused) {
            enterWaitLoop();
        }

        eventCount++;

        if (StringUtils.isBlank(line)) {
            logger.warn("Ignoring blank line: {}", line);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Handle event " + eventCount);
        }

        LogEntry newLogEntry = null;
        try {
            newLogEntry = this.jsonToObjectParser.fromJson(line, LogEntry.class);
        } catch (Throwable t) {
            logger.error("Oh boy: ", t);
        }
        this.matchOrHoldEvent(newLogEntry);
    }

    private synchronized void enterWaitLoop() {
        while(paused) {
            try {
                this.wait();
            } catch (InterruptedException ex) {
                logger.error("Unexpected exception during wait(): ", ex);
            }
        }
    }

    private void matchOrHoldEvent(LogEntry newLogEntry) {
        LogEntry otherLogEntry = unmatchedLogEvents.remove(newLogEntry.getId());
        if (otherLogEntry == null) {
            unmatchedLogEvents.put(newLogEntry.getId(), newLogEntry);
        } else {
            handleMatchedEvents(otherLogEntry, newLogEntry);
        }
    }

    private void handleMatchedEvents(LogEntry existingLogEntry, LogEntry newLogEntry) {
        EventPair eventPair = new EventPair();
        eventPair.addEvent(existingLogEntry);
        eventPair.addEvent(newLogEntry);
        this.matchedLogEventProcessor.queueLogEventPair(eventPair);
    }

    @Override
    public void handle(Exception ex) {
        logger.error("ExceptionNotified asynchronously: ",ex);
    }
}