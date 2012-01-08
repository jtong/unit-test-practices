package myClient;

import myClient.MyConnection;
import myClient.MyDriverWrapper;
import myClient.MySubscriber;
import myDriver.MyDriver;
import myDriver.MyDriverException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.initMocks;

public class MyConnectionTest {
    private MyDriverRepository myDriverRepository;

    private String[] uris;


    @Before
    public void before() {
        myDriverRepository = mock(MyDriverRepository.class);
    }

    @Test
    public void should_close_when_connect_failed() throws MyDriverException {
        uris = new String[]{
                "uri://connect.should.failed:1234",
                "uri://i.am.here.to.avoid.endless.loop"
                };
        MyDriverWrapper myDriver = mock(MyDriverWrapper.class);
        MyDriverWrapper voidEndlessLop = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(voidEndlessLop);

        doThrow(new MyDriverException("should failed")).when(myDriver).connect();

        MyConnection myConnection = new MyConnection(uris, myDriverRepository);
        myConnection.open();

        verify(myDriver).connect();
        verify(myDriver).close();
    }

    @Test
    public void should_connect_the_next_one_when_failed() throws MyDriverException {
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

        MyConnection myConnection = new MyConnection(uris, myDriverRepository);

        //when
        InOrder inOrder = inOrder(myDriver1_failed, myDriver2);
        myConnection.open();

        //then
        inOrder.verify(myDriver1_failed).connect();
        inOrder.verify(myDriver2).connect();
    }


    @Test
    public void should_not_connect_the_next_one_when_not_failed() throws MyDriverException {
        //given
        uris = new String[]{
                "uri://connect.should.failed:1",
                "uri://connect.should.never.connect:2",
        };
        MyDriverWrapper myDriver1_pass = mock(MyDriverWrapper.class);
        MyDriverWrapper myDriver2_never_connect = mock(MyDriverWrapper.class);

        when(myDriverRepository.getMyDriver(uris[0])).thenReturn(myDriver1_pass);
        when(myDriverRepository.getMyDriver(uris[1])).thenReturn(myDriver2_never_connect);

        doNothing().when(myDriver1_pass).connect();

        MyConnection myConnection = new MyConnection(uris, myDriverRepository);

        myConnection.open();

        verify(myDriver1_pass).connect();
        verify(myDriver2_never_connect, never()).connect();
    }

    @Test
    public void should_not_connect_the_first_one_when_last_one_failed() throws MyDriverException {
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

        MyConnection myConnection = new MyConnection(uris, myDriverRepository);

        //when
        InOrder inOrder = inOrder(myDriver1_failed, myDriver2_failed, myDriver3_pass);
        myConnection.open();

        inOrder.verify(myDriver1_failed).connect();
        inOrder.verify(myDriver2_failed).connect();
        inOrder.verify(myDriver3_pass).connect();
    }

}
