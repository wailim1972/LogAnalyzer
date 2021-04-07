package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import loganalyzer.dto.LogEntry;
import loganalyzer.entity.Event;
import loganalyzer.model.EventPair;
import loganalyzer.repository.EventRepository;
import loganalyzer.service.LogEventListener;
import loganalyzer.service.LogEventProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import util.LogGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={LogEventProcessor.class})
public class LogEventProcessorTest {
    @MockBean
    private LogEventListener listener;

    @MockBean
    EventRepository eventRepository;

    @Autowired
    private LogEventProcessor logEventProcessor;

    @ParameterizedTest
    @MethodSource("usageTestArgsProvider")
    public void whenBadArgsSupplied_thenCorrectExceptionIsThrown(String[] args, Class expectedThrowable)  {
        Assertions.assertThrows(expectedThrowable, () -> {
            logEventProcessor.run(args);
        });
    }

    @ParameterizedTest
    @MethodSource("mockArgsAndBatchProvider")
    public void whenEventPairIsProcessed_thenRepoIsInvokedCorectly(String[] args, List<EventPair> mockBatch) {
        try {
            Mockito.when(listener.getAndResetEventPairs()).thenReturn(mockBatch).thenThrow(new RuntimeException("STOP"));
            try {
                logEventProcessor.run(args);
            } catch(RuntimeException ex) {
                // intentional
            }

            ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventRepository).saveAll(argumentCaptor.capture());
            List<Event> capturedArgument = argumentCaptor.<List<Event>> getValue();
            Assertions.assertEquals(capturedArgument.get(0).getId(), "MockId");
        } catch (Exception ex) {
            Class clazz = ex.getClass();
        }
    }

    private static Stream<Arguments> mockArgsAndBatchProvider() {
        Gson jsonToObjectParser = new GsonBuilder().create();
        List<String> pair = new LogGenerator().generateLogRecordPair(LogGenerator.NON_APP_TEMPLATE, "MockId");
        EventPair logEventPair = new EventPair();
        logEventPair.addEvent(jsonToObjectParser.fromJson(pair.get(0), LogEntry.class));
        logEventPair.addEvent(jsonToObjectParser.fromJson(pair.get(1), LogEntry.class));
        //List<EventPair> batch = new ArrayList<EventPair>(List.of(logEventPair));
        List<EventPair> batch = new ArrayList<EventPair>();
        batch.add(logEventPair);

        return Stream.of(
                Arguments.of(new String[]{"fred"}, batch)
        );
    }

    private static Stream<Arguments> usageTestArgsProvider() {
        return Stream.of(
                Arguments.of(new String[]{""}, IllegalArgumentException.class),
                Arguments.of(new String[]{" "}, IllegalArgumentException.class),
                Arguments.of(new String[]{}, IllegalArgumentException.class),
                Arguments.of(new String[]{"fred", "fred2"}, IllegalArgumentException.class)
        );
    }
}
