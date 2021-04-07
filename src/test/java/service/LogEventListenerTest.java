package service;

import loganalyzer.service.LogEventListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={LogEventListener.class})
public class LogEventListenerTest {

    @Autowired
    private LogEventListener logEventListener;

    // Todo -> Add tests passing valid and invalid lines to ensure reasonable exception handling / behaviour
    // Todo -> Add test files with limited data, with both good and bad formatting and ensure reasonable behaviour
    @Test
    public void whenXYZ_thenXYZ() {
        this.logEventListener.handle("");
    }

}
