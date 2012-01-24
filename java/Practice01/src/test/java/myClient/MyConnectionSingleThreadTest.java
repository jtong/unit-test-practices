package myClient;

import myDriver.MyData;
import myDriver.MyDriver;
import myDriver.MyDriverException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Closeable;
import java.util.EventObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class MyConnectionSingleThreadTest {
    @Mock
    private MyDriverRepository myDriverRepository;
    @Mock
    private MyThreadUtil myThreadUtil;
    @Mock
    private MyDriverWrapper myDriver;
    @Mock
    private MySubscriber mySubscriber;

    private MyConnectionSingleThread myConnectionSingleThread;

    private String[] uris;
    private int queryId = 0;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(MyConnection.class);

    }


    @Test
    public void should_close_when_connect_failed() throws MyDriverException, InterruptedException {
        uris = new String[]{
                "uri://connect.should.failed:1234",
                "uri://i.am.here.to.avoid.endless.loop"
                };
        MyDriverWrapper myDriver = mock(MyDriverWrapper.class);
        MyDriverWrapper voidEndlessLop = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(voidEndlessLop);

        doThrow(new MyDriverException("should failed")).when(myDriver).connect();

        MyConnectionSingleThread myConnection = new MyConnectionSingleThread(uris, myDriverRepository, myThreadUtil);
        myConnection.open();

        verify(myDriver).connect();
        verify(myDriver).close();
    }

    @Test
    public void should_connect_the_next_one_when_failed() throws InterruptedException, MyDriverException {
        //given
        uris = new String[]{
                "uri://connect.should.failed:1",
                "uri://connect.should.pass:2",
        };

        MyDriverWrapper myDriver1_failed = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2 = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver1_failed);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(myDriver2);

        doThrow(new MyDriverException("should failed")).when(myDriver1_failed).connect();
        doNothing().when(myDriver2).connect();

        MyConnectionSingleThread myConnection = givenMyConnectionSingleThread();

        //when
        InOrder inOrder = inOrder(myDriver1_failed, myDriver2);
        myConnection.open();

        //then
        inOrder.verify(myDriver1_failed).connect();
        inOrder.verify(myDriver2).connect();
    }


    @Test
    public void should_not_connect_the_next_one_when_not_failed() throws MyDriverException, InterruptedException {
        //given
        uris = new String[]{
                "uri://connect.should.not.failed:1",
                "uri://connect.should.never.called:2",
        };
        MyDriverWrapper myDriver1_pass = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2_never_connect = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver1_pass);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(myDriver2_never_connect);

        doNothing().when(myDriver1_pass).connect();

        MyConnectionSingleThread myConnection = givenMyConnectionSingleThread();

        myConnection.open();

        verify(myDriver1_pass).connect();
        verify(myDriver2_never_connect, never()).connect();
    }

    @Test
    public void should_connect_the_first_one_when_last_one_failed() throws MyDriverException, InterruptedException {
        //given
        uris = new String[]{
                "uri://connect.should.failed.at.first:1",
                "uri://connect.should.failed:2",
        };

        MyDriverWrapper myDriver1_failed = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2_failed = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver3_pass = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver1_failed)
                .thenReturn(myDriver3_pass);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(myDriver2_failed);

        doThrow(new MyDriverException("should failed")).when(myDriver1_failed).connect();
        doThrow(new MyDriverException("should failed")).when(myDriver2_failed).connect();
        doNothing().when(myDriver3_pass).connect();

        MyConnectionSingleThread myConnection = givenMyConnectionSingleThread();

        //when
        InOrder inOrder = inOrder(myDriver1_failed, myDriver2_failed, myDriver3_pass);
        myConnection.open();

        inOrder.verify(myDriver1_failed).connect();
        inOrder.verify(myDriver2_failed).connect();
        inOrder.verify(myDriver3_pass).connect();
    }
    
    @Test
    public void should_dispatch_connect_failed_event_when_each_connect_failed() throws MyDriverException, InterruptedException {
        uris = new String[]{
                "uri://connect.should.failed.at.first:1",
                "uri://connect.should.failed:2",
                "uri://connect.should.pass:3"
        };

        MyDriverWrapper myDriver1_failed = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2_failed = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver3_pass = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver1_failed);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(myDriver2_failed);
        when(myDriverRepository.getMyDriver(uris[2])).thenReturn(myDriver3_pass);

        doThrow(new MyDriverException("should failed")).when(myDriver1_failed).connect();
        doThrow(new MyDriverException("should failed")).when(myDriver2_failed).connect();
        doNothing().when(myDriver3_pass).connect();

        MyConnectionEventListener myConnectionEventListener = mock(MyConnectionEventListener.class);

        MyConnectionSingleThread myConnection = givenMyConnectionSingleThread();
        myConnection.addListener(myConnectionEventListener);

        //when
        myConnection.open();

        //then
        verify(myConnectionEventListener, times(2)).connectionFailed(any(EventObject.class));
    }

    @Test
    public void should_wait_RECONNECT_INTERVAL_when_connect_failed() throws MyDriverException, InterruptedException {
        //given
        uris = new String[]{
                "uri://connect.should.failed:1",
                "uri://connect.should.pass:2",
        };

        MyDriverWrapper myDriver1_failed = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2 = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver1_failed);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(myDriver2);

        doThrow(new MyDriverException("should failed")).when(myDriver1_failed).connect();
        doNothing().when(myDriver2).connect();

        MyConnectionSingleThread myConnection = givenMyConnectionSingleThread();

        //when
        inOrder(myDriver1_failed, myDriver2);
        myConnection.open();

        //then
        verify(myThreadUtil).sleep(MyConnection.RECONNECT_INTERVAL);
    }

    private MyConnectionSingleThread givenMyConnectionSingleThread() {
        return new MyConnectionSingleThread(uris, myDriverRepository, myThreadUtil);
    }

    @Test
    public void should_call_my_driver_close_method_on_closing() throws MyDriverException, InterruptedException {
        givenSimpleMyConnectionSingleThread();

        doNothing().when(myDriver).connect();

        MyConnectionSingleThread myConnection = givenMyConnectionSingleThread();

        //when
        myConnection.open();
        myConnection.close();

        //then
        verify(myDriver).close();
    }

    @Test
    public void should_throw_exception_on_register_when_given_queryId_is_same() throws InterruptedException {
        givenSimpleMyConnectionSingleThread();

        myConnectionSingleThread.open();
        myConnectionSingleThread.register(queryId, mock(MySubscriber.class));
        try{
            myConnectionSingleThread.register(queryId, mock(MySubscriber.class));
            fail("It should throw exception when given queryId is duplicate.But it didn't");
        } catch(RuntimeException e) {

        }
    }

    @Test
    public void should_call_subscriber_on_begin_method_when_given_data_is_query_id_value_begin() throws MyDriverException, InterruptedException {
        givenSimpleMyConnectionSingleThread();
        given(myDriver.receive()).willReturn(new MyData(queryId, "begin"));

        myConnectionSingleThread.open();
        myConnectionSingleThread.register(queryId, mySubscriber);
        myConnectionSingleThread.receive();

        verify(mySubscriber).onBegin();

    }


    @Test
    public void should_call_subscriber_on_message_method_when_given_data_is_not_query_id_value_begin() throws MyDriverException, InterruptedException {
        givenSimpleMyConnectionSingleThread();

        MyData myData = new MyData(queryId, "123456");
        given(myDriver.receive()).willReturn(myData);

        myConnectionSingleThread.open();
        myConnectionSingleThread.register(queryId, mySubscriber);
        myConnectionSingleThread.receive();

        verify(mySubscriber).onMessage(myData.value);
    }

    private void givenSimpleMyConnectionSingleThread(){
        uris = new String[]{
                "uri://connect:1"
        };
        given(myDriverRepository.getMyDriver(uris[0])).willReturn(myDriver);
        myConnectionSingleThread = new MyConnectionSingleThread(uris, myDriverRepository, myThreadUtil);
    }

    @Test
    public void should_change_started_status_after_call_isStart_method(){
        givenSimpleMyConnectionSingleThread();

        assertThat(myConnectionSingleThread.isStart(), is(false));
        assertThat(myConnectionSingleThread.isStart(), is(true));

    }

    @Test
    public void should_get_closeable_object_after_register() throws InterruptedException {
        givenSimpleMyConnectionSingleThread();
        Closeable expectCloser = new SubscribeCloser(queryId, myConnectionSingleThread);
        myConnectionSingleThread.open();
        Closeable actualCloser = myConnectionSingleThread.register(queryId, mySubscriber);
    
        assertThat(actualCloser, is(expectCloser));
    }
    
    @Test
    public void should_remove_query_on_removing_subscriber() throws MyDriverException, InterruptedException {
        givenSimpleMyConnectionSingleThread();
        myConnectionSingleThread.open();
        myConnectionSingleThread.removeSubscriber(queryId);

        verify(myDriver).removeQuery(queryId);
    }
    
    @Test
    public void should_reconnect_when_my_driver_receive_failed_on_my_connection_single_thread_receiving() throws InterruptedException, MyDriverException {
        uris = new String[]{
                "uri://connect:1",
                "uri://connect:2"
        };
        MyDriverWrapper myDriver1 = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2 = mock(MyDriverWrapper.class);

        given(myDriverRepository.getMyDriver(uris[0])).willReturn(myDriver1);
        given(myDriverRepository.getMyDriver(uris[1])).willReturn(myDriver2);
        myConnectionSingleThread = new MyConnectionSingleThread(uris, myDriverRepository, myThreadUtil);
        myConnectionSingleThread.open();

        doThrow(new MyDriverException("connect failed at second time")).when(myDriver1).connect();

        given(myDriver1.receive()).willThrow(new MyDriverException("receive failed"));

        myConnectionSingleThread.receive();

        verify(myDriver1, times(2)).connect();
        verify(myDriver2).connect();
    }
}
