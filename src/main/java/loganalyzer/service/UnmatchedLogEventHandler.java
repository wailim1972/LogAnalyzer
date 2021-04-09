package loganalyzer.service;

public interface UnmatchedLogEventHandler {
    void run(String... args) throws IllegalArgumentException;
    void handle(String line);
    void pause();
    void resume();
}
