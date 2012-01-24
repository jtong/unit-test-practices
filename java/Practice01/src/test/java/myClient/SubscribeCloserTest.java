package myClient;

import myDriver.MyDriverException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class SubscribeCloserTest {
    @Test
    public void should_remove_subscriber_when_close() throws IOException, MyDriverException {
        Integer queryId = 0;
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        SubscribeCloser closer = new SubscribeCloser(queryId, myConnectionSingleThread);

        closer.close();

        verify(myConnectionSingleThread).removeSubscriber(queryId);
    }
}
