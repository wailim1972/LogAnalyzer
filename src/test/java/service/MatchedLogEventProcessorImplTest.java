package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import loganalyzer.dto.LogEntry;
import loganalyzer.entity.Event;
import loganalyzer.model.EventPair;
import loganalyzer.repository.EventRepository;
import loganalyzer.service.impl.UnmatchedLogEventListenerImpl;
import loganalyzer.service.impl.MatchedLogEventProcessorImpl;
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
@ContextConfiguration(classes={MatchedLogEventProcessorImpl.class})
public class MatchedLogEventProcessorImplTest {
    @MockBean
    private UnmatchedLogEventListenerImpl listener;

    @MockBean
    EventRepository eventRepository;

    @Autowired
    private MatchedLogEventProcessorImpl logEventProcessor;


    @ParameterizedTest
    @MethodSource("mockArgsAndBatchProvider")
    public void whenEventPairIsProcessed_thenRepoIsInvokedCorectly(String[] args, EventPair mockEventPair) {
        logEventProcessor.processLogEventPair(mockEventPair);

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(argumentCaptor.capture());
        Event capturedEvent = argumentCaptor.getValue();

        Assertions.assertEquals(capturedEvent.getId(), mockEventPair.getId());
    }

    private static Stream<Arguments> mockArgsAndBatchProvider() {
        Gson jsonToObjectParser = new GsonBuilder().create();
        List<String> pair = new LogGenerator().generateLogRecordPair(LogGenerator.NON_APP_TEMPLATE, "MockId");
        EventPair logEventPair = new EventPair();
        logEventPair.addEvent(jsonToObjectParser.fromJson(pair.get(0), LogEntry.class));
        logEventPair.addEvent(jsonToObjectParser.fromJson(pair.get(1), LogEntry.class));

        return Stream.of(
                Arguments.of(new String[]{"fred"}, logEventPair)
        );
    }

}
