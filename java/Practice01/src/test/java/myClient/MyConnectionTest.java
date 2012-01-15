package myClient;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;

import java.awt.*;
import java.util.EventObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class MyConnectionTest {
    @Test
    public void should_call_single_thread_delegate_event_on_opening() throws InterruptedException {
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.open();

        verify(myConnectionSingleThread, timeout(500)).open();
    }

    @Test
    public void should_dispatch_connected_event_after_connected() throws InterruptedException {
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        MyConnectionEventListener listener = mock(MyConnectionEventListener.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);
        myConnection.addConnectionListener(listener);

        myConnection.open();

        ArgumentCaptor<EventObject> eventObjectArgumentCaptor = ArgumentCaptor.forClass(EventObject.class);
        verify(listener, timeout(1000)).connected(eventObjectArgumentCaptor.capture());
        assertThat(eventObjectArgumentCaptor.getValue().toString(), is(new EventObject(myConnection).toString()));
    }

    @Test
    public void should_add_event_listener_to_single_thread_delegate_class_when_add_listener(){
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        MyConnectionEventListener listener = mock(MyConnectionEventListener.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.addConnectionListener(listener);

        verify(myConnectionSingleThread).addListener(listener);
    }

    @Test
    public void should_call_my_connection_single_thread_close_method_on_closing() throws InterruptedException {
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.close();

        verify(myConnectionSingleThread).close();
    }

    @Test
    public void should_dispatch_disconnected_event_after_closed() throws InterruptedException {
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        MyConnectionEventListener listener = mock(MyConnectionEventListener.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);
        myConnection.addConnectionListener(listener);

        myConnection.close();

        ArgumentCaptor<EventObject> eventObjectArgumentCaptor = ArgumentCaptor.forClass(EventObject.class);
        verify(listener).disconnected(eventObjectArgumentCaptor.capture());
        assertThat(eventObjectArgumentCaptor.getValue().toString(), is(new EventObject(myConnection).toString()));
    }

    @Test
    public void should_call_single_thread_class_register_method_on_subscribing(){
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        int queryId = 0;
        MySubscriber mySubscriber = mock(MySubscriber.class);

        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.subscribe(queryId, mySubscriber);

        verify(myConnectionSingleThread).register(queryId, mySubscriber);
    }

    @Test
    public void should_call_single_thread_class_subscribe_method_on_subscribing(){
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        int queryId = 0;
        MySubscriber mySubscriber = mock(MySubscriber.class);

        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.subscribe(queryId, mySubscriber);

        verify(myConnectionSingleThread).receive();
    }


}
