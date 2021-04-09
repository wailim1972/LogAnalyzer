package loganalyzer.service;

import loganalyzer.model.EventPair;

public interface MatchedLogEventProcessor {
    void queueLogEventPair(EventPair eventPair);
    void processLogEventPair(EventPair eventPair);
    void outputStats();
}
