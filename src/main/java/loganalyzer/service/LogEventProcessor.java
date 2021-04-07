package loganalyzer.service;

import loganalyzer.entity.Event;
import loganalyzer.model.EventPair;
import loganalyzer.repository.EventRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogEventProcessor implements CommandLineRunner
{
    private static final Logger logger = LoggerFactory.getLogger(LogEventListener.class);
    private static final int BATCH_WAIT_TIME_IN_MILLIS = 20;

    @Autowired
    private LogEventListener logEventListener;

    @Autowired
    EventRepository eventRepository;

    private AtomicLong eventPairCount = new AtomicLong(0);
    private int processedBatches = 0;

    @Override
    public void run(String... args) throws IllegalArgumentException {
        if (args.length != 1 || StringUtils.isBlank(args[0])) {
            String msg = "Usage: gradlew -Pparams=<filepath> bootRun";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        } else {
            this.analyzeLog(args[0]);
        }
    }

    private void analyzeLog(String filename) {
        this.logEventListener.tailFile(filename);

        try {
            // Come back to this 'true' to control exit with a state & accessor
            while (true) {
                // Don't hog a CPU Core unless its idle
                Thread.yield();

                List<EventPair> batch = logEventListener.getAndResetEventPairs();
                if (batch.isEmpty()) {
                    System.out.println("No Event Pairs to process. Sleeping.");
                    Thread.sleep(BATCH_WAIT_TIME_IN_MILLIS);
                    continue;
                }

                this.processedBatches++;
                processEventPairBatch(batch);
            }
        } catch (Exception ex) {
            System.out.println("Lumped all unhandled Exceptions here but obviously could be more advanced: " + ex);
            ex.printStackTrace(System.out);
        }
        finally {
            this.logEventListener.stop();
        }
    }

    private void processEventPairBatch(List<EventPair> batch) {
        long start = System.currentTimeMillis();
        List<Event> events = Collections.synchronizedList(new ArrayList<>(batch.size()));
        // Split the conversion to Entities across available CPU Cores
        batch.parallelStream().forEach(E -> events.add(Event.fromEventPair(E)));
        // Return to single threaded for persistence phase to avoid table locks
        this.eventRepository.saveAll(events);
        long totalEventsProcessed = this.eventPairCount.addAndGet(batch.size());
        long end = System.currentTimeMillis();
        long duration = (end - start);
        System.out.println("Processed batch: " + this.processedBatches + " in " + duration
                + " milliseconds, (total matched pairs: " + totalEventsProcessed + ")");
    }
}
