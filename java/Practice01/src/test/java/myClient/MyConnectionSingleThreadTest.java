package myClient;

import myDriver.MyDriverException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.EventObject;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class MyConnectionSingleThreadTest {
    @Mock
    private MyDriverRepository myDriverRepository;
    @Mock
    private MyThreadUtil myThreadUtil;

    private String[] uris;

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
    public void should_not_connect_the_first_one_when_last_one_failed() throws MyDriverException, InterruptedException {
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

}
