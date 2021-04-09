package service;

import loganalyzer.service.MatchedLogEventProcessor;
import loganalyzer.service.UnmatchedLogEventHandler;
import loganalyzer.service.impl.UnmatchedLogEventListenerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={UnmatchedLogEventListenerImpl.class})
public class UnmatchedLogEventListenerImplTest {

    @MockBean
    private MatchedLogEventProcessor matchedLogEventProcessor;

    @Autowired
    private UnmatchedLogEventHandler unmatchedLogEventHandler;

    // Todo -> Add tests passing valid and invalid lines to ensure reasonable exception handling / behaviour
    // Todo -> Add test files with limited data, with both good and bad formatting and ensure reasonable behaviour

    private static Stream<Arguments> usageTestArgsProvider() {
        return Stream.of(
                Arguments.of(new String[]{""}, IllegalArgumentException.class),
                Arguments.of(new String[]{" "}, IllegalArgumentException.class),
                Arguments.of(new String[]{}, IllegalArgumentException.class),
                Arguments.of(new String[]{"fred", "fred2"}, IllegalArgumentException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("usageTestArgsProvider")
    public void whenBadArgsSupplied_thenCorrectExceptionIsThrown(String[] args, Class expectedThrowable)  {
        Assertions.assertThrows(expectedThrowable, () -> {
            this.unmatchedLogEventHandler.run(args);
        });
    }

}
