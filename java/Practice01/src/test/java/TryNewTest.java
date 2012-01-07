import myClient.MyConnection;
import myClient.MySubscriber;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class TryNewTest {
    @Test
    public void should_run_in_mave_test_phase(){
        MyConnection myConnection = Mockito.mock(MyConnection.class);
        Mockito.when(myConnection.subscribe(1, null)).thenReturn(null);
    }
}
