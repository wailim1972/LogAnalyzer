package loganalyzer.service.impl;

import loganalyzer.entity.Event;
import loganalyzer.model.EventPair;
import loganalyzer.repository.EventRepository;
import loganalyzer.service.MatchedLogEventProcessor;
import loganalyzer.service.UnmatchedLogEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MatchedLogEventProcessorImpl implements MatchedLogEventProcessor
{
    private static final Logger logger = LoggerFactory.getLogger(MatchedLogEventProcessorImpl.class);
    // Some high and low 'water marks' to control backpressure to the log tailer thread,
    // if the persistence is falling behind. This avoids potential memory issues.
    private static final long PAUSE_THRESHOLD = 4000;
    private static final long RESUME_THRESHOLD = 200;

    @Autowired
    private UnmatchedLogEventHandler unmatchedLogEventHandler;

    @Autowired
    EventRepository eventRepository;

    private ExecutorService executor;

    private AtomicLong totalQueuedEvents = new AtomicLong(0);
    private AtomicLong totalProcessedEvents = new AtomicLong(0);
    private AtomicLong currentQueueEvents = new AtomicLong(0);

    private AtomicBoolean handlerPaused = new AtomicBoolean(false);

    MatchedLogEventProcessorImpl() {
        logger.info("MatchedLogEventProcessorImpl({})", this);
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void processLogEventPair(EventPair eventPair) {
        if (logger.isDebugEnabled()) {
            logger.debug("processLogEventPair({})", eventPair.getId());
        }

        totalProcessedEvents.incrementAndGet();
        long currentQueueSize = currentQueueEvents.decrementAndGet();
        if (this.handlerPaused.get() && RESUME_THRESHOLD >= currentQueueSize) {
            logger.info("Handler now resuming log parsing.");
            unmatchedLogEventHandler.resume();
            handlerPaused.set(false);
        }

        Event event = Event.fromEventPair(eventPair);
        this.eventRepository.save(event);

        // Output some basic stats every 200 events. Would be better to use a regular timer
        if (totalProcessedEvents.intValue() % 200 == 0) {
            this.outputStats();
        }
    }

    @Override
    public void outputStats() {
        logger.info("Stats: Total Processed: {}, Currently Queued: {}", this.totalProcessedEvents, this.currentQueueEvents);
    }

    @Override
    public void queueLogEventPair(EventPair eventPair) {
        totalQueuedEvents.incrementAndGet();
        long currentQueueSize = currentQueueEvents.incrementAndGet();
        if (currentQueueSize >= PAUSE_THRESHOLD) {
            logger.warn("Handler paused as database writes are not keeping up. May need investigation.");
            unmatchedLogEventHandler.pause();
            handlerPaused.set(true);
        }

        if (eventPair.getDuration() > 4) {
            logger.warn("Detected long running event: {}", eventPair);
        }

        executor.submit(() -> {
            this.processLogEventPair(eventPair);
        });
    }
}