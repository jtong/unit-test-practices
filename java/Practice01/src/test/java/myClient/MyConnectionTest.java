package myClient;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.awt.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.EventObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MyConnectionTest {
    private int queryId = 0;
    @Mock
    private MySubscriber mySubscriber;
    @Mock
    private MyConnectionSingleThread myConnectionSingleThread;

    @Before
    public void before(){
        MockitoAnnotations.initMocks(MyConnection.class);
    }

    @Test
    public void should_call_single_thread_delegate_event_on_opening() throws InterruptedException {
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.open();

        verify(myConnectionSingleThread, timeout(500)).open();
    }

    @Test
    public void should_dispatch_connected_event_after_connected() throws InterruptedException {
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
        MyConnectionEventListener listener = mock(MyConnectionEventListener.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.addConnectionListener(listener);

        verify(myConnectionSingleThread).addListener(listener);
    }

    @Test
    public void should_call_my_connection_single_thread_close_method_on_closing() throws InterruptedException {
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.close();

        verify(myConnectionSingleThread).close();
    }

    @Test
    public void should_dispatch_disconnected_event_after_closed() throws InterruptedException {
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
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.subscribe(queryId, mySubscriber);

        verify(myConnectionSingleThread).register(queryId, mySubscriber);
    }

    @Test
    public void should_call_single_thread_class_subscribe_method_on_subscribing() throws InterruptedException {
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        myConnection.subscribe(queryId, mySubscriber);

        Thread.sleep(100);
        verify(myConnectionSingleThread, atLeastOnce()).receive();
    }

    @Test
    public void should_get_clone_able_from_single_thread_delegate_after_register(){
        MyConnectionSingleThread myConnectionSingleThread = mock(MyConnectionSingleThread.class);
        MyConnection myConnection = new MyConnection(myConnectionSingleThread);

        Closeable givenCloseable = new Closeable() {
            @Override
            public void close() throws IOException {}
        };
        given(myConnectionSingleThread.register(queryId, mySubscriber)).willReturn(givenCloseable);

        Closeable actualCloseable = myConnection.subscribe(queryId, mySubscriber);

        assertThat(actualCloseable, is(givenCloseable));
    }

}
